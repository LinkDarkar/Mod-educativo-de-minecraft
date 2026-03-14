package net.linkdarkar.testmod.scripting;

import net.minecraft.entity.LivingEntity;
import java.util.Map;

public class ScriptVerifier {
    public record VerificationResult(boolean isCorrect, String message) {}

    public static VerificationResult verify(ScriptBlock userScript, ScriptBlock correctScript, LivingEntity dummyEntity) {
        ExecutionContext userCtx = new ExecutionContext(dummyEntity);
        userCtx.isSimulation = true;

        ExecutionContext correctCtx = new ExecutionContext(dummyEntity);
        correctCtx.isSimulation = true;

        try {
            correctScript.Execute(correctCtx);
        } catch (Exception e) {
            return new VerificationResult(false, "Teacher's script failed: " + e.getMessage());
        }

        try {
            userScript.Execute(userCtx);
        } catch (Exception e) {
            return new VerificationResult(false, "Your script crashed: " + e.getMessage());
        }

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
                return new VerificationResult(false, "Wrong PRINT output. Expected output: '" + expectedMsg + "'");
            }
        }

        boolean checkedAnyVariables = false;
        for (Map.Entry<String, Object> expectedEntry : correctCtx.variables.entrySet()) {
            String varName = expectedEntry.getKey();

            // Skips variables starting with _ so they are not used to verify
            if (varName.startsWith("_")) continue;

            checkedAnyVariables = true;
            Object expectedValue = expectedEntry.getValue();
            Object userValue = userCtx.variables.get(varName);

            if (userValue == null) {
                return new VerificationResult(false, "Missing expected variable: '" + varName + "'");
            }

            if (!expectedValue.toString().equals(userValue.toString())) {
                return new VerificationResult(false, "Variable '" + varName + "' has the wrong value. Expected value: " + expectedValue + ", Got: " + userValue);
            }
        }

        if (correctCtx.printedMessages.isEmpty() && !checkedAnyVariables) {
            return new VerificationResult(true, "Script ran successfully without crashing!");
        }

        for (Map.Entry<String, Object> expectedEntry : correctCtx.variables.entrySet()) {
            String varName = expectedEntry.getKey();
            Object expectedValue = expectedEntry.getValue();
            Object userValue = userCtx.variables.get(varName);

            if (userValue == null) {
                return new VerificationResult(false, "Missing expected variable: '" + varName + "'");
            }

            if (!expectedValue.toString().equals(userValue.toString())) {
                return new VerificationResult(false, "Variable '" + varName + "' has the wrong value. Expected value: " + expectedValue + ", Got: " + userValue);
            }
        }

        return new VerificationResult(true, "Correct! Output and variables match.");
    }
}