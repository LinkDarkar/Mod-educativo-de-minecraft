package net.linkdarkar.testmod.scripting;

import net.minecraft.entity.LivingEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ScriptVerifier {
    public static class VerificationReport {
        public boolean isCorrect;
        public String summaryMessage;
        public List<TestCaseResult> caseResults = new ArrayList<>();
    }

    public static class TestCaseResult {
        public int caseNumber;
        public boolean passed;
        public String errorReason = "";

        public Map<String, String> expectedVars = new LinkedHashMap<>();
        public Map<String, String> actualVars = new LinkedHashMap<>();

        public List<String> expectedPrints = new ArrayList<>();
        public List<String> actualPrints = new ArrayList<>();

        public List<String> expectedActions = new ArrayList<>();
        public List<String> actualActions = new ArrayList<>();
    }

    public static VerificationReport verify(ScriptBlock userScript, ScriptBlock correctScript, LivingEntity dummyEntity) {
        VerificationReport report = new VerificationReport();

        ScriptingConfigManager.ScriptingConfig config = ScriptingConfigManager.getInstance().getConfig(dummyEntity.getUuid());
        List<ScriptingConfigManager.TestCase> cases = config.testCases;

        // If no scenarios are defined, we create a blank dummy test case so it runs at least once
        if (cases.isEmpty()) {
            cases = new ArrayList<>();
            cases.add(new ScriptingConfigManager.TestCase());
        }

        int caseNumber = 1;
        boolean allPassed = true;

        for (ScriptingConfigManager.TestCase testCase : cases) {
            TestCaseResult tr = new TestCaseResult();
            tr.caseNumber = caseNumber;
            tr.passed = true;

            ExecutionContext userCtx = new ExecutionContext(dummyEntity);
            userCtx.isSimulation = true;

            ExecutionContext correctCtx = new ExecutionContext(dummyEntity);
            correctCtx.isSimulation = true;

            // Inject the starting variables
            for (ScriptingConfigManager.PersistentVariable pv : testCase.variables) {
                if (pv.name == null || pv.name.trim().isEmpty()) continue;
                String cleanName = pv.name.trim();

                try {
                    double num = Double.parseDouble(pv.value);
                    userCtx.SetVar(cleanName, num);
                    correctCtx.SetVar(cleanName, num);
                } catch (NumberFormatException e) {
                    userCtx.SetVar(cleanName, pv.value);
                    correctCtx.SetVar(cleanName, pv.value);
                }
            }

            // Run Teacher Script
            try {
                correctScript.Execute(correctCtx);
            } catch (Exception e) {
                tr.passed = false;
                tr.errorReason = "Teacher's script crashed: " + e.getMessage();
                report.caseResults.add(tr);
                allPassed = false;
                break;
            }

            // Run User Script
            try {
                userScript.Execute(userCtx);
            } catch (Exception e) {
                tr.passed = false;
                tr.errorReason = "Your script crashed: " + e.getMessage();
                report.caseResults.add(tr);
                allPassed = false;
                break;
            }

            // STRICT ACTION VERIFICATION
            tr.expectedActions.addAll(correctCtx.recordedActions);
            tr.actualActions.addAll(userCtx.recordedActions);

            List<String> expectedActions = correctCtx.recordedActions;
            List<String> actualActions = userCtx.recordedActions;

            int minActionSize = Math.min(expectedActions.size(), actualActions.size());

            for (int i = 0; i < minActionSize; i++) {
                if (!expectedActions.get(i).equals(actualActions.get(i))) {
                    tr.passed = false;
                    tr.errorReason = "Action mismatch. Expected to: '" + expectedActions.get(i) + "', but tried to: '" + actualActions.get(i) + "'";
                    break;
                }
            }

            if (tr.passed && expectedActions.size() != actualActions.size()) {
                tr.passed = false;
                if (actualActions.size() < expectedActions.size()) {
                    tr.errorReason = "Missing actions. Expected " + expectedActions.size() + " actions, got " + actualActions.size() + ".";
                } else {
                    tr.errorReason = "Too many actions performed.";
                }
            }

            // Record Output for the UI
            tr.expectedPrints.addAll(correctCtx.printedMessages);
            tr.actualPrints.addAll(userCtx.printedMessages);

            for (Map.Entry<String, Object> expectedEntry : correctCtx.variables.entrySet()) {
                String varName = expectedEntry.getKey().trim();
                if (!varName.startsWith("_") || varName.startsWith("_MOCK_")) continue; // Only track vars with "_"

                tr.expectedVars.put(varName, expectedEntry.getValue().toString());
                Object userVal = userCtx.variables.get(varName);
                tr.actualVars.put(varName, userVal != null ? userVal.toString() : "UNDEFINED");
            }

            // STRICT PRINT VERIFICATION
            List<String> expectedPrints = correctCtx.printedMessages;
            List<String> actualPrints = userCtx.printedMessages;

            int minSize = Math.min(expectedPrints.size(), actualPrints.size());

            // Check for any mismatches in the overlapping sequence
            for (int i = 0; i < minSize; i++) {
                String expectedMsg = expectedPrints.get(i);
                String actualMsg = actualPrints.get(i);

                if (!expectedMsg.equals(actualMsg)) {
                    tr.passed = false;
                    tr.errorReason = "Print #" + (i + 1) + " mismatch. Expected: '" + expectedMsg + "', Got: '" + actualMsg + "'";
                    break;
                }
            }

            // If the sequence matched perfectly so far, check if the total amount of prints is correct
            if (tr.passed && expectedPrints.size() != actualPrints.size()) {
                tr.passed = false;
                if (actualPrints.size() < expectedPrints.size()) {
                    tr.errorReason = "Missing prints. Expected " + expectedPrints.size() + " total, but got " + actualPrints.size() + ". (Next expected was: '" + expectedPrints.get(actualPrints.size()) + "')";
                } else {
                    tr.errorReason = "Too many prints. Expected " + expectedPrints.size() + " total, but got " + actualPrints.size() + ". (Extra print: '" + actualPrints.get(expectedPrints.size()) + "')";
                }
            }

            // Verify Variables (Only if Prints already passed)
            if (tr.passed) {
                for (Map.Entry<String, Object> expectedEntry : correctCtx.variables.entrySet()) {
                    String varName = expectedEntry.getKey().trim();
                    if (!varName.startsWith("_") || varName.startsWith("_MOCK_")) continue;

                    Object expectedValue = expectedEntry.getValue();
                    Object userValue = userCtx.variables.get(varName);

                    if (userValue == null) {
                        tr.passed = false;
                        tr.errorReason = "Missing expected variable: '" + varName + "'";
                        break;
                    }

                    boolean match;
                    if (expectedValue instanceof Number && userValue instanceof Number) {
                        match = ((Number) expectedValue).doubleValue() == ((Number) userValue).doubleValue();
                    } else {
                        match = expectedValue.toString().replace("\"", "").equals(userValue.toString().replace("\"", ""));
                    }

                    if (!match) {
                        tr.passed = false;
                        tr.errorReason = "Variable mismatch on '" + varName + "'";
                        break;
                    }
                }
            }

            report.caseResults.add(tr);
            if (!tr.passed) allPassed = false;
            caseNumber++;
        }

        report.isCorrect = allPassed;
        if (allPassed) {
            report.summaryMessage = "Correct! Passed all " + cases.size() + " test case scenario(s).";
        } else {
            report.summaryMessage = "Failed. Check the verification report for details.";
        }

        return report;
    }
}