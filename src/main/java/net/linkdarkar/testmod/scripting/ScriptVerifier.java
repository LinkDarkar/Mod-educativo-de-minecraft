package net.linkdarkar.testmod.scripting;

import net.minecraft.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ScriptVerifier {
    public record VerificationResult(boolean isCorrect, String message) {}

    public static VerificationResult verify(ScriptBlock userScript, ScriptBlock correctScript, LivingEntity dummyEntity) {
        ScriptingConfigManager.ScriptingConfig config = ScriptingConfigManager.getInstance().getConfig(dummyEntity.getUuid());
        List<ScriptingConfigManager.TestCase> cases = config.testCases;

        // If no scenarios are defined, we create a blank dummy test case so it runs at least once
        if (cases.isEmpty()) {
            cases = new ArrayList<>();
            cases.add(new ScriptingConfigManager.TestCase());
        }

        int caseNumber = 1;
        for (ScriptingConfigManager.TestCase testCase : cases) {

            ExecutionContext userCtx = new ExecutionContext(dummyEntity);
            userCtx.isSimulation = true;

            ExecutionContext correctCtx = new ExecutionContext(dummyEntity);
            correctCtx.isSimulation = true;

            // Inject the starting variables for this specific Test Case scenario
            for (ScriptingConfigManager.PersistentVariable pv : testCase.variables) {
                try {
                    double num = Double.parseDouble(pv.value);
                    userCtx.SetVar(pv.name, num);
                    correctCtx.SetVar(pv.name, num);
                } catch (NumberFormatException e) {
                    userCtx.SetVar(pv.name, pv.value);
                    correctCtx.SetVar(pv.name, pv.value);
                }
            }

            try {
                correctScript.Execute(correctCtx);
            } catch (Exception e) {
                return new VerificationResult(false, "[Case " + caseNumber + "] Teacher's script failed: " + e.getMessage());
            }

            try {
                userScript.Execute(userCtx);
            } catch (Exception e) {
                return new VerificationResult(false, "[Case " + caseNumber + "] Your script crashed: " + e.getMessage());
            }

            // Check printed outputs
            int userPrintIndex = 0;
            for (String expectedMsg : correctCtx.printedMessages) {
                boolean found = false;
                while (userPrintIndex < userCtx.printedMessages.size()) {
                    if (userCtx.printedMessages.get(userPrintIndex).equals(expectedMsg)) {
                        found = true;
                        userPrintIndex++;
                        break;
                    }
                    userPrintIndex++;
                }

                if (!found) {
                    return new VerificationResult(false, "[Case " + caseNumber + "] Wrong PRINT output. Expected output: '" + expectedMsg + "'");
                }
            }

            // Check resulting variables
            for (Map.Entry<String, Object> expectedEntry : correctCtx.variables.entrySet()) {
                String varName = expectedEntry.getKey();

                if (varName.startsWith("_")) continue;

                Object expectedValue = expectedEntry.getValue();
                Object userValue = userCtx.variables.get(varName);

                if (userValue == null) {
                    return new VerificationResult(false, "[Case " + caseNumber + "] Missing expected variable: '" + varName + "'");
                }

                if (!expectedValue.toString().equals(userValue.toString())) {
                    return new VerificationResult(false, "[Case " + caseNumber + "] Variable '" + varName + "' has wrong value. Expected: " + expectedValue + ", Got: " + userValue);
                }
            }

            caseNumber++;
        }

        return new VerificationResult(true, "Correct! Passed all " + cases.size() + " test case scenario(s).");
    }
}