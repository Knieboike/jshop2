package JSHOP2;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Vector;

import static JSHOP2.DomainJsonConverter.vectorToJsonArray;

/**
 * JSONParser for JSHOP2
 */
public class JSONParser {

    // Main method
    public static String generateJSON(InternalDomain domain, InternalDomain problem) {
        String domainName = getDomainName(domain);

        return new JsonBuilder()
                .startObject()
                .addTitle(domainName)
                .startObject()
                .addRawProperty("requirements", generateRequirements(domain))
                .addRawProperty("problem", createIntegratedProblemObject(problem, domain)) // Pass domain for variable names
                .addRawProperty("domain", createDomainObject(domain))
                .endObject()
                .endObject()
                .toString();
    }

    public static void exportDomainToJson(InternalDomain domain, String filename) throws IOException {
        String json = new JsonBuilder()
                .startObject()
                .addTitle(domain.getName())
                .startObject()
                .addRawProperty("requirements", "[]") // TODO: Add actual requirements if available
                .addRawProperty("domain", createDomainObject(domain))
                .endObject()
                .endObject()
                .toString();

        writeToFile(json, filename);
    }

    // New method for integrated JSON export matching Scala version
    public static void exportIntegratedJson(InternalDomain domain, InternalDomain problem, String filename) throws IOException {
        String json = generateJSON(domain, problem);
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

    // Generate requirements array (placeholder for now)
    private static String generateRequirements(InternalDomain domain) {
        // TODO: Extract actual requirements from domain if available
        return "[]";
    }

    // Create integrated problem
    private static String createIntegratedProblemObject(InternalDomain problem, InternalDomain domain) {
        return new JsonBuilder()
                .startObject()
                .addRawProperty("goal", createGoalObject(problem, domain))
                .addRawProperty("init", createInitArray(problem, domain))
                .endObject()
                .toString();
    }

    // Fixed goal object creation
    private static String createGoalObject(InternalDomain problem, InternalDomain domain) {
        return new JsonBuilder()
                .startObject()
                .addRawProperty("tasks", createGoalTasksArray(problem, domain))
                .endObject()
                .toString();
    }

    // Fixed goal tasks array creation
    private static String createGoalTasksArray(InternalDomain problem, InternalDomain domain) {
        JsonBuilder builder = new JsonBuilder().startArray();

        TaskList goalTasks = getGoalTasksFromProblem(problem);
        if (goalTasks != null && goalTasks != TaskList.empty) {
            addTasksToJsonArray(builder, goalTasks, domain); // Use domain for variable names
        }

        return builder.endArray().toString();
    }

    // Fixed init array creation
    private static String createInitArray(InternalDomain problem, InternalDomain domain) {
        JsonBuilder builder = new JsonBuilder().startArray();

        Vector<Predicate> initialState = getInitialStateFromProblem(problem);
        if (initialState != null && !initialState.isEmpty()) {
            for (Predicate predicate : initialState) {
                if (predicate != null) {
                    builder.addRawArrayElement(predicateToJsonForInit(predicate, domain)); // Use domain for variable names
                }
            }
        }

        return builder.endArray().toString();
    }

    private static String createDomainObject(InternalDomain domain) {
        JsonBuilder builder = new JsonBuilder()
                .startObject()
                .addProperty("name", getDomainName(domain))
                .addRawProperty("primitive_tasks", createOperatorsArray(domain))
                .addRawProperty("compound_tasks", createCompoundTasksArray(domain));

        return builder.endObject().toString();
    }

    // Operators array creation
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

    // Compound tasks (methods) array creation
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

    // Problem JSON handling - Updated to use actual data if available
    private static String addProblemToExistingJson(String existingJson, InternalDomain problem) {
        String baseContent = removeClosingBrace(existingJson.trim());
        String problemJson = createProblemObjectWithActualData(problem);
        return baseContent + ",\n  \"problem\": " + problemJson + "\n}";
    }

    private static String createProblemObjectWithActualData(InternalDomain problem) {
        return new JsonBuilder()
                .startObject()
                .addRawProperty("goal", createGoalObjectWithActualData(problem))
                .addRawProperty("init", createInitArrayWithActualData(problem))
                .endObject()
                .toString();
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

        // Try to get goal tasks from the problem
        TaskList goalTasks = getGoalTasksFromProblem(problem);
        if (goalTasks != null && goalTasks != TaskList.empty) {
            addTasksToJsonArray(builder, goalTasks, problem);
        } else {
            // Create example goal task based on domain compound tasks
            Vector<String> compoundTasks = problem.getCompoundTasks();
            if (compoundTasks != null && !compoundTasks.isEmpty()) {
                String taskName = compoundTasks.get(0); // Use first compound task as example
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

        // Try to get initial state from the problem
        Vector<Predicate> initialState = getInitialStateFromProblem(problem);
        if (initialState != null && !initialState.isEmpty()) {
            for (Predicate predicate : initialState) {
                if (predicate != null) {
                    builder.addRawArrayElement(predicateToJson(predicate, problem));
                }
            }
        } else {
            // Create example predicate based on domain constants
            Vector<String> constants = problem.getConstants();
            if (constants != null && !constants.isEmpty()) {
                String constName = constants.get(0); // Use first constant as example
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
            // This is an atomic task
            builder.addRawArrayElement(taskAtomToJson(taskAtom, domain));
        } else if (taskList.subtasks != null) {
            // This is a compound task list
            for (TaskList subtask : taskList.subtasks) {
                if (subtask != null) {
                    addTasksToJsonArray(builder, subtask, domain);
                }
            }
        }
    }


    // Helper methods for converting objects to JSON
    private static String taskAtomToJson(TaskAtom taskAtom, InternalDomain domain) {
        String taskName = getTaskName(taskAtom, domain);
        String parameters = getTaskParameters(taskAtom, domain);

        return new JsonBuilder()
                .startObject()
                .addProperty("name", taskName)
                .addProperty("type", "predicate")
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

    // Enhanced parameter extraction for init predicates (returns string array, not object array)
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
            // Fallback
        }
        return "[]";
    }

    // New method to convert terms to string array instead of object array
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

    // Helper method to recursively add term list elements as strings
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
                    .addProperty("term", "unknown")
                    .addProperty("type", "unknown")
                    .endObject()
                    .toString();
        }
        if (parameter instanceof TermVariable) {
            TermVariable var = (TermVariable) parameter;
            return new JsonBuilder()
                    .startObject()
                    .addProperty("term", getVariableName(var, domain))
                    .addProperty("type", "Variable")
                    .endObject()
                    .toString();
        } else if (parameter instanceof TermConstant) {
            TermConstant constant = (TermConstant) parameter;
            return new JsonBuilder()
                    .startObject()
                    .addProperty("term", getConstantName(constant, domain))
                    .addProperty("type", "Constant")
                    .endObject()
                    .toString();
        } else {
            return new JsonBuilder()
                    .startObject()
                    .addProperty("term", parameter.toString())
                    .addProperty("type", "Term")
                    .endObject()
                    .toString();
        }
    }

    // Helper methods to extract data from domain and problem objects
    private static TaskList getGoalTasksFromProblem(InternalDomain problem) {
        // Get actual goal tasks from stored problem data
        return problem.getFirstGoalTaskList();
    }

    private static Vector<Predicate> getInitialStateFromProblem(InternalDomain problem) {
        // Get actual initial state from stored problem data
        return problem.getFirstInitialState();
    }

    private static String getTaskName(TaskAtom taskAtom, InternalDomain domain) {
        // Extract task name from TaskAtom
        try {
            if (taskAtom != null && taskAtom.getHead() != null) {
                int taskIndex = taskAtom.getHead().getHead();

                // Check if this is a primitive task or compound task
                if (taskAtom.isPrimitive()) {
                    // Use primitive tasks list
                    if (taskIndex >= 0 && taskIndex < domain.getPrimitiveTasks().size()) {
                        return domain.getPrimitiveTasks().get(taskIndex);
                    }
                } else {
                    // Use compound tasks list
                    if (taskIndex >= 0 && taskIndex < domain.getCompoundTasks().size()) {
                        return domain.getCompoundTasks().get(taskIndex);
                    }
                }
            }
        } catch (Exception e) {
            // Fallback
        }
        return "unknown_task";
    }

    private static String getPredicateName(Predicate predicate, InternalDomain domain) {
        // Extract predicate name
        try {
            return domain.getConstants().get(predicate.getHead());
        } catch (Exception e) {
            return "unknown_predicate";
        }
    }

    private static String getTaskParameters(TaskAtom taskAtom, InternalDomain domain) {
        // Extract parameters from TaskAtom
        try {
            if (taskAtom != null && taskAtom.getHead() != null) {
                Term param = taskAtom.getHead().getParam();
                return termToParametersArray(param, domain);
            }
        } catch (Exception e) {
            // Fallback
        }
        return "[]";
    }

    private static String getPredicateParameters(Predicate predicate, InternalDomain domain) {
        // Extract parameters from Predicate
        try {
            Term param = predicate.getParam();
            if (param instanceof TermList) {
                return termToParametersArray(param, domain);
            } else if (param != null) {
                // For simple parameters, extract the string directly
                JsonBuilder builder = new JsonBuilder().startArray();
                String paramStr = getConstantNameFromTerm(param, domain);
                builder.addRawArrayElement("\"" + paramStr + "\"");
                return builder.endArray().toString();
            }
        } catch (Exception e) {
            // Fallback
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
                // Get the first element
                Term first = termList.getFirst();
                if (first != null) {
                    builder.addRawArrayElement(parameterToJson(first, domain));
                }

                // Get the rest of the list
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

    // Utility methods
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
            // Hole den Namen aus der Variablenliste der Domain, falls vorhanden
            String name = domain.getVariableName(variable.getIndex());
            if (name != null && !name.isEmpty()) {
                // Stelle sicher, dass der Name mit '?' beginnt (wie in der Domain-Beschreibung)
                if (!name.startsWith("?")) {
                    name = "?" + name;
                }
                return name;
            }
        } catch (Exception e) {
            // fallback
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
