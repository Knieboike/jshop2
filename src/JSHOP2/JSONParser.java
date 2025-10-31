package JSHOP2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import static JSHOP2.DomainJsonConverter.vectorToJsonArray;

/**
 * Converts JSHOP2 domain and problem data to JSON format
 */
public class JSONParser {

    public static void exportDomainToJson(InternalDomain domain, String filename) throws IOException {
        String json = new JsonBuilder()
                .startObject()
                .addTitle(domain.getName())
                .startObject()
                .addRawProperty("domain", createDomainObject(domain))
                .endObject()
                .endObject()
                .toString();

        writeToFile(json, filename);
    }

    public static void exportProblemOnlyToJson(InternalDomain problem, String filename) throws IOException {
        String json = "{\n" +
                "  \"problem\": {\n" +
                "    \"name\": \"" + (problem.getProbName() != null ? problem.getProbName() : "unknown") + "\",\n" +
                "    \"additional_constants\": " + vectorToJsonArray(problem.getAdditionalConstants(), "constants") + "\n" +
                "  }\n" +
                "}";

        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        writer.write(json);
        writer.close();
    }

    public static void appendProblemToJson(InternalDomain problem, String filename) throws IOException {
        String existingContent = readExistingFile(filename);
        String updatedContent = addProblemToExistingJson(existingContent, problem);
        writeToFile(updatedContent, filename);
    }

    public static void exportProblemToJson(InternalDomain problem, InternalDomain domain, String filename) throws IOException {
        String domainName = getDomainName(domain);

        String json = new JsonBuilder()
                .startObject()
                .addTitle(domainName)
                .startObject()
                .addRawProperty("problem", createIntegratedProblemObject(problem, domain))
                .endObject()
                .endObject()
                .toString();

        writeToFile(json, filename);
    }

    public static void appendDomainToJson(InternalDomain domain, String filename) throws IOException {
        String existingContent = readExistingFile(filename);
        String updatedContent = addDomainToExistingJson(existingContent, domain);
        writeToFile(updatedContent, filename);
    }

    private static String generateRequirements(InternalDomain domain) {
        return DomainJsonConverter.extractRequirements(domain);
    }

    private static String createIntegratedProblemObject(InternalDomain problem, InternalDomain domain) {
        return new JsonBuilder()
                .startObject()
                .addRawProperty("requirements", generateRequirements(domain))
                .addRawProperty("goal", createGoalObject(problem, domain))
                .addRawProperty("init", createInitArray(problem, domain))
                .endObject()
                .toString();
    }

    private static String createGoalObject(InternalDomain problem, InternalDomain domain) {
        return new JsonBuilder()
                .startObject()
                .addRawProperty("tasks", createGoalTasksArray(problem, domain))
                .endObject()
                .toString();
    }

    private static String createGoalTasksArray(InternalDomain problem, InternalDomain domain) {
        JsonBuilder builder = new JsonBuilder().startArray();

        TaskList goalTasks = getGoalTasksFromProblem(problem);
        if (goalTasks != null && goalTasks != TaskList.empty) {
            addTasksToJsonArray(builder, goalTasks, domain);
        }

        return builder.endArray().toString();
    }

    private static String createInitArray(InternalDomain problem, InternalDomain domain) {
        JsonBuilder builder = new JsonBuilder().startArray();

        Vector<Predicate> initialState = getInitialStateFromProblem(problem);
        if (initialState != null && !initialState.isEmpty()) {
            for (Predicate predicate : initialState) {
                if (predicate != null) {
                    builder.addRawArrayElement(predicateToJsonForInit(predicate, domain));
                }
            }
        }

        return builder.endArray().toString();
    }

    private static String createDomainObject(InternalDomain domain) {
        JsonBuilder builder = new JsonBuilder()
                .startObject()
                .addRawProperty("requirements", generateRequirements(domain))
                .addProperty("name", getDomainName(domain))
                .addRawProperty("primitive_tasks", createOperatorsArray(domain))
                .addRawProperty("compound_tasks", createCompoundTasksArray(domain));

        return builder.endObject().toString();
    }

    private static String createOperatorsArray(InternalDomain domain) {
        JsonBuilder builder = new JsonBuilder().startArray();

        Vector<InternalOperator> operators = domain.getOperators();
        for (InternalOperator operator : operators) {
            if (operator != null) {
                builder.addRawArrayElement(DomainJsonConverter.operatorToJson(operator, domain));
            }
        }

        return builder.endArray().toString();
    }

    private static String createCompoundTasksArray(InternalDomain domain) {
        JsonBuilder builder = new JsonBuilder().startArray();

        Vector<InternalMethod> methods = domain.getMethods();
        for (InternalMethod method : methods) {
            if (method != null) {
                builder.addRawArrayElement(DomainJsonConverter.methodToJson(method, domain));
            }
        }

        return builder.endArray().toString();
    }

    private static String addProblemToExistingJson(String existingJson, InternalDomain problem) {
        String requirements = extractRequirementsFromJson(existingJson);

        int domainIndex = existingJson.indexOf("\"domain\"");

        if (domainIndex != -1) {
            String beforeDomain = existingJson.substring(0, domainIndex);
            String fromDomain = existingJson.substring(domainIndex);

            String problemJson = createProblemObjectWithRequirements(problem, requirements);
            return beforeDomain + "\"problem\": " + problemJson + ",\n  " + fromDomain;
        } else {
            String baseContent = removeClosingBrace(existingJson.trim());
            String problemJson = createProblemObjectWithRequirements(problem, requirements);
            return baseContent + ",\n  \"problem\": " + problemJson + "\n}";
        }
    }

    private static String addDomainToExistingJson(String existingJson, InternalDomain domain) {
        String baseContent = removeClosingBrace(existingJson.trim());
        String domainJson = createDomainObject(domain);
        return baseContent + ",\n  \"domain\": " + domainJson + "\n}";
    }

    private static String createProblemObjectWithRequirements(InternalDomain problem, String requirements) {
        return new JsonBuilder()
                .startObject()
                .addRawProperty("requirements", requirements)
                .addRawProperty("goal", createGoalObjectWithActualData(problem))
                .addRawProperty("init", createInitArrayWithActualData(problem))
                .endObject()
                .toString();
    }

    private static String extractRequirementsFromJson(String json) {
        try {
            int reqStart = json.indexOf("\"requirements\"");
            if (reqStart == -1) {
                return "[]";
            }

            int colonIndex = json.indexOf(":", reqStart);
            if (colonIndex == -1) {
                return "[]";
            }

            int bracketStart = json.indexOf("[", colonIndex);
            if (bracketStart == -1) {
                return "[]";
            }

            int bracketEnd = findMatchingBracket(json, bracketStart);
            if (bracketEnd == -1) {
                return "[]";
            }

            return json.substring(bracketStart, bracketEnd + 1);
        } catch (Exception e) {
            return "[]";
        }
    }

    private static int findMatchingBracket(String json, int openBracketIndex) {
        int depth = 0;
        for (int i = openBracketIndex; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static String createGoalObjectWithActualData(InternalDomain problem) {
        return new JsonBuilder()
                .startObject()
                .addRawProperty("tasks", createGoalTasksArrayWithActualData(problem))
                .endObject()
                .toString();
    }

    private static String createGoalTasksArrayWithActualData(InternalDomain problem) {
        JsonBuilder builder = new JsonBuilder().startArray();

        TaskList goalTasks = getGoalTasksFromProblem(problem);
        if (goalTasks != null && goalTasks != TaskList.empty) {
            addTasksToJsonArray(builder, goalTasks, problem);
        } else {
            Vector<String> compoundTasks = problem.getCompoundTasks();
            if (compoundTasks != null && !compoundTasks.isEmpty()) {
                String taskName = compoundTasks.get(0);
                String taskJson = new JsonBuilder()
                        .startObject()
                        .addProperty("name", taskName)
                        .addProperty("type", "predicate")
                        .addRawProperty("parameters", "did not work")
                        .endObject()
                        .toString();
                builder.addRawArrayElement(taskJson);
            }
        }

        return builder.endArray().toString();
    }

    private static String createInitArrayWithActualData(InternalDomain problem) {
        JsonBuilder builder = new JsonBuilder().startArray();

        Vector<Predicate> initialState = getInitialStateFromProblem(problem);
        if (initialState != null && !initialState.isEmpty()) {
            for (Predicate predicate : initialState) {
                if (predicate != null) {
                    builder.addRawArrayElement(predicateToJson(predicate, problem));
                }
            }
        } else {
            Vector<String> constants = problem.getConstants();
            if (constants != null && !constants.isEmpty()) {
                String constName = constants.get(0);
                String predicateJson = new JsonBuilder()
                        .startObject()
                        .addProperty("name", constName)
                        .addRawProperty("parameters", "did not work")
                        .endObject()
                        .toString();
                builder.addRawArrayElement(predicateJson);
            }
        }

        return builder.endArray().toString();
    }

    private static void addTasksToJsonArray(JsonBuilder builder, TaskList taskList, InternalDomain domain) {
        if (taskList == null || taskList == TaskList.empty) return;

        TaskAtom taskAtom = taskList.getTask();
        if (taskAtom != null) {
            builder.addRawArrayElement(taskAtomToJson(taskAtom, domain));
        } else if (taskList.subtasks != null) {
            for (TaskList subtask : taskList.subtasks) {
                if (subtask != null) {
                    addTasksToJsonArray(builder, subtask, domain);
                }
            }
        }
    }

    private static String taskAtomToJson(TaskAtom taskAtom, InternalDomain domain) {
        String taskName = getTaskName(taskAtom, domain);
        String parameters = getTaskParameters(taskAtom, domain);
        String taskType = taskAtom.isPrimitive() ? "predicate" : "task";

        return new JsonBuilder()
                .startObject()
                .addProperty("name", taskName)
                .addProperty("type", taskType)
                .addRawProperty("parameters", parameters)
                .endObject()
                .toString();
    }

    private static String predicateToJson(Predicate predicate, InternalDomain domain) {
        String predicateName = getPredicateName(predicate, domain);
        String parameters = getPredicateParameters(predicate, domain);

        return new JsonBuilder()
                .startObject()
                .addProperty("name", predicateName)
                .addRawProperty("parameters", parameters)
                .addProperty("type", "predicate")
                .endObject()
                .toString();
    }

    private static String predicateToJsonForInit(Predicate predicate, InternalDomain domain) {
        String predicateName = getPredicateName(predicate, domain);
        String parameters = getPredicateParametersAsStringArray(predicate, domain);

        return new JsonBuilder()
                .startObject()
                .addProperty("predicate", predicateName)
                .addRawProperty("parameters", parameters)
                .endObject()
                .toString();
    }

    private static String getPredicateParametersAsStringArray(Predicate predicate, InternalDomain domain) {
        try {
            Term param = predicate.getParam();
            if (param instanceof TermList) {
                return termToStringArray(param, domain);
            } else if (param != null) {
                JsonBuilder builder = new JsonBuilder().startArray();
                String paramStr = getConstantNameFromTerm(param, domain);
                builder.addRawArrayElement("\"" + paramStr + "\"");
                return builder.endArray().toString();
            }
        } catch (Exception e) {
            // Fall through to default
        }
        return "[]";
    }

    private static String termToStringArray(Term term, InternalDomain domain) {
        if (term == null) return "[]";

        JsonBuilder builder = new JsonBuilder().startArray();

        if (term instanceof TermList) {
            TermList termList = (TermList) term;
            addTermListToStringArray(builder, termList, domain);
        } else {
            String termStr = getConstantNameFromTerm(term, domain);
            builder.addRawArrayElement("\"" + termStr + "\"");
        }

        return builder.endArray().toString();
    }

    private static void addTermListToStringArray(JsonBuilder builder, TermList termList, InternalDomain domain) {
        if (termList == null || termList.isEmpty()) return;

        Term first = termList.getFirst();
        if (first != null) {
            String termStr = getConstantNameFromTerm(first, domain);
            builder.addRawArrayElement("\"" + termStr + "\"");
        }

        Term rest = termList.getRest();
        if (rest instanceof TermList) {
            addTermListToStringArray(builder, (TermList) rest, domain);
        } else if (rest != null && !(rest instanceof TermConstant && ((TermConstant) rest).getIndex() == -1)) {
            String termStr = getConstantNameFromTerm(rest, domain);
            builder.addRawArrayElement("\"" + termStr + "\"");
        }
    }

    private static String parameterToJson(Term parameter, InternalDomain domain) {
        if (parameter == null) {
            return new JsonBuilder()
                    .startObject()
                    .addProperty("name", "unknown")
                    .addProperty("type", "unknown")
                    .endObject()
                    .toString();
        }
        if (parameter instanceof TermVariable) {
            TermVariable var = (TermVariable) parameter;
            return new JsonBuilder()
                    .startObject()
                    .addProperty("name", getVariableName(var, domain))
                    .addProperty("type", "Variable")
                    .endObject()
                    .toString();
        } else if (parameter instanceof TermConstant) {
            TermConstant constant = (TermConstant) parameter;
            return new JsonBuilder()
                    .startObject()
                    .addProperty("name", getConstantName(constant, domain))
                    .addProperty("type", "Constant")
                    .endObject()
                    .toString();
        } else {
            return new JsonBuilder()
                    .startObject()
                    .addProperty("name", parameter.toString())
                    .addProperty("type", "Term")
                    .endObject()
                    .toString();
        }
    }

    private static TaskList getGoalTasksFromProblem(InternalDomain problem) {
        return problem.getFirstGoalTaskList();
    }

    private static Vector<Predicate> getInitialStateFromProblem(InternalDomain problem) {
        return problem.getFirstInitialState();
    }

    private static String getTaskName(TaskAtom taskAtom, InternalDomain domain) {
        try {
            if (taskAtom != null && taskAtom.getHead() != null) {
                int taskIndex = taskAtom.getHead().getHead();

                if (taskAtom.isPrimitive()) {
                    if (taskIndex >= 0 && taskIndex < domain.getPrimitiveTasks().size()) {
                        return domain.getPrimitiveTasks().get(taskIndex);
                    }
                } else {
                    if (taskIndex >= 0 && taskIndex < domain.getCompoundTasks().size()) {
                        return domain.getCompoundTasks().get(taskIndex);
                    }
                }
            }
        } catch (Exception e) {
            // Fall through to default
        }
        return "unknown_task";
    }

    private static String getPredicateName(Predicate predicate, InternalDomain domain) {
        try {
            return domain.getConstants().get(predicate.getHead());
        } catch (Exception e) {
            return "unknown_predicate";
        }
    }

    private static String getTaskParameters(TaskAtom taskAtom, InternalDomain domain) {
        try {
            if (taskAtom != null && taskAtom.getHead() != null) {
                Term param = taskAtom.getHead().getParam();
                return termToParametersArray(param, domain);
            }
        } catch (Exception e) {
            // Fall through to default
        }
        return "[]";
    }

    private static String getPredicateParameters(Predicate predicate, InternalDomain domain) {
        try {
            Term param = predicate.getParam();
            if (param instanceof TermList) {
                return termToParametersArray(param, domain);
            } else if (param != null) {
                JsonBuilder builder = new JsonBuilder().startArray();
                String paramStr = getConstantNameFromTerm(param, domain);
                builder.addRawArrayElement("\"" + paramStr + "\"");
                return builder.endArray().toString();
            }
        } catch (Exception e) {
            // Fall through to default
        }
        return "[]";
    }

    private static String getConstantNameFromTerm(Term term, InternalDomain domain) {
        if (term instanceof TermConstant) {
            TermConstant constant = (TermConstant) term;
            try {
                return domain.getConstants().get(constant.getIndex());
            } catch (Exception e) {
                return term.toString();
            }
        }
        return term.toString();
    }

    private static String termToParametersArray(Term term, InternalDomain domain) {
        if (term == null) return "[]";

        JsonBuilder builder = new JsonBuilder().startArray();

        if (term instanceof TermList) {
            TermList termList = (TermList) term;

            if (!termList.isEmpty()) {
                Term first = termList.getFirst();
                if (first != null) {
                    builder.addRawArrayElement(parameterToJson(first, domain));
                }

                Term rest = termList.getRest();
                if (rest != null && rest instanceof TermList) {
                    String restJson = termToParametersArray(rest, domain);
                    if (!restJson.equals("[]")) {
                        String innerContent = restJson.substring(1, restJson.length() - 1);
                        if (!innerContent.trim().isEmpty()) {
                            String[] elements = innerContent.split(",(?=\\s*\\{)");
                            for (String element : elements) {
                                if (!element.trim().isEmpty()) {
                                    builder.addRawArrayElement(element.trim());
                                }
                            }
                        }
                    }
                } else if (rest != null && !(rest instanceof TermConstant && ((TermConstant) rest).getIndex() == -1)) {
                    builder.addRawArrayElement(parameterToJson(rest, domain));
                }
            }
        } else {
            builder.addRawArrayElement(parameterToJson(term, domain));
        }

        return builder.endArray().toString();
    }

    private static String getDomainName(InternalDomain domain) {
        return domain.getName() != null ? domain.getName() : "unknown_domain";
    }

    private static String readExistingFile(String filename) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = reader.readLine()) != null) {
            content.append(line).append("\n");
        }
        reader.close();
        return content.toString();
    }

    private static void writeToFile(String content, String filename) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        writer.write(content);
        writer.close();
    }

    private static String removeClosingBrace(String json) {
        int lastBrace = json.lastIndexOf('}');
        return lastBrace > 0 ? json.substring(0, lastBrace) : json;
    }

    private static String getVariableName(TermVariable variable, InternalDomain domain) {
        try {
            String name = domain.getVariableName(variable.getIndex());
            if (name != null && !name.isEmpty()) {
                if (!name.startsWith("?")) {
                    name = "?" + name;
                }
                return name;
            }
        } catch (Exception e) {
            // Fall through to default
        }
        return "?var" + variable.getIndex();
    }

    private static String getConstantName(TermConstant constant, InternalDomain domain) {
        try {
            return domain.getConstants().get(constant.getIndex());
        } catch (Exception e) {
            return constant.toString();
        }
    }
}
