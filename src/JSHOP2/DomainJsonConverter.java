package JSHOP2;

import java.util.HashSet;
import java.util.Set;
import java.util.Vector;

import static JSHOP2.ParameterJsonConverter.parametersToJsonArray;

/**
 * Converts domain elements (operators, methods, tasks) to JSON format
 */
public class DomainJsonConverter {

    public static String operatorToJson(InternalOperator operator, InternalDomain domain) {
        String predicateName = getPredicateName(operator.getHead().getHead(), domain.getPrimitiveTasks());
        String parameters = parametersToJsonArray(operator.getHead().getParam(), domain);

        return new JsonBuilder()
                .startObject()
                .addProperty("name", predicateName)
                .addRawProperty("parameters", parameters)
                .addRawProperty("preconditions", preconditionToJson(operator.getPre(), domain))
                .addRawProperty("effect", effectsToJson(operator, domain))
                .endObject()
                .toString();
    }

    static String methodToJson(InternalMethod method, InternalDomain domain) {
        String methodName = extractMethodName(method, domain);
        String parameters = extractMethodParameters(method, domain);
        String methodsArray = buildMethodsArray(method, domain);

        return new JsonBuilder()
                .startObject()
                .addProperty("name", methodName)
                .addRawProperty("parameters", parameters)
                .addRawProperty("methods", methodsArray)
                .endObject()
                .toString();
    }

    private static String extractMethodName(InternalMethod method, InternalDomain domain) {
        try {
            if (method.getHead() != null) {
                Predicate pred = method.getHead();
                int headIndex = pred.getHead();
                Vector<String> compoundTasks = domain.getCompoundTasks();
                if (headIndex >= 0 && headIndex < compoundTasks.size()) {
                    return compoundTasks.get(headIndex);
                } else {
                    return "method_" + headIndex;
                }
            }
        } catch (Exception e) {
            return "error_method_" + method.getCnt();
        }
        return "unknown";
    }

    private static String extractMethodParameters(InternalMethod method, InternalDomain domain) {
        try {
            if (method.getHead() != null && method.getHead().getParam() != null) {
                return parametersToJsonArray(method.getHead().getParam(), domain);
            }
        } catch (Exception e) {
            // Fall through to default
        }
        return "[]";
    }

    private static String buildMethodsArray(InternalMethod method, InternalDomain domain) {
        JsonBuilder builder = new JsonBuilder().startArray();

        Vector<String> labels = method.getLabels();
        Vector<LogicalPrecondition> pres = method.getPres();
        Vector<TaskList> subs = method.getSubs();

        for (int i = 0; i < pres.size(); i++) {
            String branchJson = new JsonBuilder()
                    .startObject()
                    .addProperty("name", labels.get(i))
                    .addRawProperty("preconditions", safePreconditionToJson(pres.get(i), domain))
                    .addRawProperty("tasks", safeTaskListToJson(subs.get(i), domain))
                    .endObject()
                    .toString();

            builder.addRawArrayElement(branchJson);
        }

        return builder.endArray().toString();
    }

    private static String safePreconditionToJson(LogicalPrecondition precondition, InternalDomain domain) {
        try {
            return precondition != null ? preconditionToJson(precondition, domain) : "null";
        } catch (Exception e) {
            return new JsonBuilder()
                    .startObject()
                    .addProperty("type", "error")
                    .addProperty("message", e.getMessage())
                    .endObject()
                    .toString();
        }
    }

    private static String safeTaskListToJson(TaskList taskList, InternalDomain domain) {
        try {
            return taskList != null ? taskListToJson(taskList, domain) : "null";
        } catch (Exception e) {
            return new JsonBuilder()
                    .startObject()
                    .addProperty("type", "error")
                    .addProperty("message", e.getMessage())
                    .endObject()
                    .toString();
        }
    }

    public static String vectorToJsonArray(Vector<String> vector, String elementType) {
        JsonBuilder builder = new JsonBuilder().startArray();

        for (String item : vector) {
            if ("primitive_task".equals(elementType)) {
                String taskJson = new JsonBuilder()
                        .startObject()
                        .addProperty("name", item)
                        .addProperty("type", "primitive_task")
                        .endObject()
                        .toString();
                builder.addRawArrayElement(taskJson);
            } else {
                builder.addArrayElement(item);
            }
        }

        return builder.endArray().toString();
    }

    private static String preconditionToJson(LogicalPrecondition precondition, InternalDomain domain) {
        if (precondition == null) {
            return "null";
        }

        return ExpressionJsonConverter.expressionToJson(precondition.getExpression(), domain);
    }

    private static String effectsToJson(InternalOperator operator, InternalDomain domain) {
        JsonBuilder effectsBuilder = new JsonBuilder().startArray();


        Vector<?> deleteList = InternalOperator.getDel();
        if (deleteList != null && !deleteList.isEmpty()) {
            String deleteEffects = delAddListToJsonEffects(deleteList, domain, "delete");
            if (deleteEffects != null && !deleteEffects.equals("[]")) {
                String effectsContent = deleteEffects.substring(1, deleteEffects.length() - 1); // Remove [ and ]
                if (!effectsContent.trim().isEmpty()) {
                    effectsBuilder.addRawArrayElement(effectsContent);
                }
            }
        }

        Vector<?> addList = InternalOperator.getAdd();
        if (addList != null && !addList.isEmpty()) {
            String addEffects = delAddListToJsonEffects(addList, domain, "add");
            if (addEffects != null && !addEffects.equals("[]")) {
                String effectsContent = addEffects.substring(1, addEffects.length() - 1); // Remove [ and ]
                if (!effectsContent.trim().isEmpty()) {
                    effectsBuilder.addRawArrayElement(effectsContent);
                }
            }
        }

        return effectsBuilder.endArray().toString();
    }

    private static String delAddListToJsonEffects(Vector<?> delAddList, InternalDomain domain, String effectType) {
        JsonBuilder builder = new JsonBuilder().startArray();
        boolean hasEffects = false;

        if (delAddList != null && !delAddList.isEmpty()) {
            Object firstElement = delAddList.get(0);

            if (firstElement instanceof Integer) {
                Integer varIdx = (Integer) firstElement;
                String variableJson = new JsonBuilder()
                        .startObject()
                        .addProperty("type", "variable")
                        .addProperty("index", varIdx)
                        .endObject()
                        .toString();
                builder.addRawArrayElement(variableJson);
                hasEffects = true;
            } else {
                for (Object element : delAddList) {
                    if (element instanceof DelAddElement) {
                        String elementJson = effectToJson((DelAddElement) element, domain, effectType);
                        builder.addRawArrayElement(elementJson);
                        hasEffects = true;
                    }
                }
            }
        }
        return hasEffects ? builder.endArray().toString() : null;
    }

    private static String effectToJson(DelAddElement effect, InternalDomain domain, String effectType) {
        String className = effect.getClass().getSimpleName();
        switch (className) {
            case "DelAddAtomic":
                return atomicEffectToJson((DelAddAtomic) effect, domain, effectType);
            case "DelAddForAll":
                return forallEffectToJson((DelAddForAll) effect, effectType);
            case "DelAddProtection":
                return protectionEffectToJson((DelAddProtection) effect, effectType);
            default:
                return new JsonBuilder()
                        .startObject()
                        .addProperty("type", "unknown")
                        .addProperty("class", effect.getClass().getSimpleName())
                        .endObject()
                        .toString();
        }
    }

    private static String atomicEffectToJson(DelAddAtomic atomicEffect, InternalDomain domain, String effectType) {
        Predicate atom = atomicEffect.getAtom();
        String predicateName = getPredicateName(atom.getHead(), domain.getPrimitiveTasks());
        String parameters = ParameterJsonConverter.parametersToJsonArray(atom.getParam(), domain);

        if ("delete".equals(effectType)) {
            return new JsonBuilder()
                    .startObject()
                    .addProperty("type", "not")
                    .addRawProperty("expression", new JsonBuilder()
                            .startArray()
                            .addRawArrayElement(new JsonBuilder()
                                    .startObject()
                                    .addProperty("type", "predicate")
                                    .addProperty("name", predicateName)
                                    .addRawProperty("parameters", parameters)
                                    .endObject()
                                    .toString())
                            .endArray()
                            .toString())
                    .endObject()
                    .toString();
        } else {
            return new JsonBuilder()
                    .startObject()
                    .addProperty("type", "predicate")
                    .addProperty("name", predicateName)
                    .addRawProperty("parameters", parameters)
                    .endObject()
                    .toString();
        }
    }

    private static String forallEffectToJson(DelAddForAll forallEffect, String effectType) {
        try {
            LogicalExpression expression = forallEffect.getExpression();
            Predicate[] atoms = forallEffect.getAtoms();
            JsonBuilder variablesBuilder = new JsonBuilder().startArray();

            String variablesJson = variablesBuilder.endArray().toString();

            String expressionJson = "null";
            if (expression != null) {
                expressionJson = ExpressionJsonConverter.expressionToJson(expression, null);
            }

            JsonBuilder predicatesBuilder = new JsonBuilder().startArray();
            if (atoms != null) {
                for (Predicate atom : atoms) {
                    String predicateName = "unknown";
                    try {
                        int headIndex = atom.getHead();
                        predicateName = "pred_" + headIndex;
                    } catch (Exception e) {
                        predicateName = "unknown_predicate";
                    }

                    String parameters = "[]";
                    try {
                        parameters = ParameterJsonConverter.parametersToJsonArray(atom.getParam(), null);
                    } catch (Exception e) {
                        // Fall through to default
                    }

                    String atomJson;
                    if ("delete".equals(effectType)) {
                        atomJson = new JsonBuilder()
                                .startObject()
                                .addProperty("type", "not")
                                .addRawProperty("expression", new JsonBuilder()
                                        .startArray()
                                        .addRawArrayElement(new JsonBuilder()
                                                .startObject()
                                                .addProperty("type", "predicate")
                                                .addProperty("name", predicateName)
                                                .addRawProperty("parameters", parameters)
                                                .endObject()
                                                .toString())
                                        .endArray()
                                        .toString())
                                .endObject()
                                .toString();
                    } else {
                        atomJson = new JsonBuilder()
                                .startObject()
                                .addProperty("type", "predicate")
                                .addProperty("name", predicateName)
                                .addRawProperty("parameters", parameters)
                                .endObject()
                                .toString();
                    }

                    predicatesBuilder.addRawArrayElement(atomJson);
                }
            }
            String predicatesJson = predicatesBuilder.endArray().toString();

            return new JsonBuilder()
                    .startObject()
                    .addProperty("type", "forall")
                    .addRawProperty("variables", variablesJson)
                    .addRawProperty("expression", expressionJson)
                    .addRawProperty("predicates", predicatesJson)
                    .addProperty("add_list", effectType.equals("add") ? "true" : "false")
                    .endObject()
                    .toString();
        } catch (Exception e) {
            return new JsonBuilder()
                    .startObject()
                    .addProperty("type", "error")
                    .addProperty("message", e.getMessage())
                    .endObject()
                    .toString();
        }
    }

    private static String protectionEffectToJson(DelAddProtection protectionEffect, String effectType) {
        return new JsonBuilder()
                .startObject()
                .addProperty("type", "protection")
                .addProperty("effect_type", effectType)
                .addProperty("message", "Protection effects implementation pending")
                .endObject()
                .toString();
    }

    private static String getPredicateName(int headIndex, Vector<String> tasks) {
        if (tasks != null && headIndex >= 0 && headIndex < tasks.size()) {
            return tasks.get(headIndex);
        }
        return "unknown_" + headIndex;
    }

    /**
     * Analyzes the domain and extracts requirements based on used features
     */
    public static String extractRequirements(InternalDomain domain) {
        Set<String> requirements = new HashSet<>();

        // Always include STRIPS as base requirement
        requirements.add(":S");

        analyzeOperators(domain, requirements);

        analyzeMethods(domain, requirements);

        JsonBuilder builder = new JsonBuilder().startArray();
        for (String req : requirements) {
            builder.addArrayElement(req);
        }
        return builder.endArray().toString();
    }

    private static void analyzeOperators(InternalDomain domain, Set<String> requirements) {
        Vector<InternalOperator> operators = domain.getOperators();
        if (operators == null) return;

        for (InternalOperator operator : operators) {
            if (operator.getPre() != null) {
                analyzePrecondition(operator.getPre(), requirements);
            }
            analyzeEffects(operator, requirements);
        }
    }

    private static void analyzeMethods(InternalDomain domain, Set<String> requirements) {
        Vector<InternalMethod> methods = domain.getMethods();
        if (methods == null) return;

        for (InternalMethod method : methods) {
            Vector<LogicalPrecondition> preconditions = method.getPres();
            if (preconditions != null) {
                for (LogicalPrecondition pre : preconditions) {
                    analyzePrecondition(pre, requirements);
                }
            }
        }
    }

    private static void analyzePrecondition(LogicalPrecondition precondition, Set<String> requirements) {
        if (precondition == null || precondition.getExpression() == null) return;

        analyzeLogicalExpression(precondition.getExpression(), requirements);
    }

    private static void analyzeLogicalExpression(LogicalExpression expr, Set<String> requirements) {
        if (expr == null) return;

        if (expr instanceof LogicalExpressionNegation) {
            requirements.add(":NP");
            LogicalExpressionNegation negation = (LogicalExpressionNegation) expr;
            analyzeLogicalExpression(negation.getExpression(), requirements);
        }
        else if (expr instanceof LogicalExpressionDisjunction) {
            requirements.add(":DP");
            LogicalExpressionDisjunction disjunction = (LogicalExpressionDisjunction) expr;
            LogicalExpression[] expressions = disjunction.getExpression();
            if (expressions != null) {
                for (LogicalExpression e : expressions) {
                    analyzeLogicalExpression(e, requirements);
                }
            }
        }
        else if (expr instanceof LogicalExpressionConjunction) {
            LogicalExpressionConjunction conjunction = (LogicalExpressionConjunction) expr;
            LogicalExpression[] expressions = conjunction.getExpression();
            if (expressions != null) {
                for (LogicalExpression e : expressions) {
                    analyzeLogicalExpression(e, requirements);
                }
            }
        }
        else if (expr instanceof LogicalExpressionForAll) {
            requirements.add(":UP");
            LogicalExpressionForAll forall = (LogicalExpressionForAll) expr;
            analyzeLogicalExpression(forall.getPremise(), requirements);
            analyzeLogicalExpression(forall.getConsequence(), requirements);
        }
        else if (expr instanceof LogicalExpressionAssignment) {
            requirements.add(":E");
        } else if (expr instanceof LogicalExpressionCall) {
            requirements.add(":F");
        }
    }

    private static void analyzeEffects(InternalOperator operator, Set<String> requirements) {
        Vector<?> addList = InternalOperator.getAdd();
        Vector<?> delList = InternalOperator.getDel();

        if (addList != null) {
            for (Object effect : addList) {
                if (effect instanceof DelAddForAll) {
                    requirements.add(":CE");
                    requirements.add(":UP");
                }
            }
        }

        if (delList != null) {
            for (Object effect : delList) {
                if (effect instanceof DelAddForAll) {
                    requirements.add(":CE");
                    requirements.add(":UP");
                }
            }
        }
    }

    private static String taskListToJson(TaskList taskList, InternalDomain domain) {
        JsonBuilder builder = new JsonBuilder().startArray();

        try {
            addAllTasksToBuilder(taskList, builder, domain);
        } catch (Exception e) {
            String errorJson = new JsonBuilder()
                    .startObject()
                    .addProperty("type", "error")
                    .addProperty("message", e.getMessage())
                    .endObject()
                    .toString();
            builder.addRawArrayElement(errorJson);
        }

        return builder.endArray().toString();
    }

    /**
     * Recursively extracts all tasks from a TaskList and adds them to the builder
     */
    private static void addAllTasksToBuilder(TaskList taskList, JsonBuilder builder, InternalDomain domain) {
        if (taskList == null || taskList == TaskList.empty) {
            return;
        }

        TaskAtom taskAtom = taskList.getTask();
        if (taskAtom != null) {
            builder.addRawArrayElement(taskElementToJson(taskAtom, domain));
            return;
        }

        if (taskList.subtasks != null && taskList.subtasks.length > 0) {
            for (TaskList subtask : taskList.subtasks) {
                if (subtask != null) {
                    addAllTasksToBuilder(subtask, builder, domain);
                }
            }
        }
    }

    private static String taskElementToJson(Object taskElement, InternalDomain domain) {
        try {
            if (taskElement instanceof Predicate) {
                Predicate pred = (Predicate) taskElement;
                String taskName = getTaskName(pred, domain);
                String parameters = ParameterJsonConverter.parametersToJsonArrayForTasks(pred.getParam(), domain);

                return new JsonBuilder()
                        .startObject()
                        .addProperty("name", taskName)
                        .addProperty("type", "predicate")
                        .addRawProperty("parameters", parameters)
                        .endObject()
                        .toString();

            } else if (taskElement instanceof TaskAtom) {
                TaskAtom atom = (TaskAtom) taskElement;
                Predicate head = atom.getHead();

                if (head != null) {
                    String taskName = getTaskName(head, domain);
                    String parameters = ParameterJsonConverter.parametersToJsonArrayForTasks(head.getParam(), domain);

                    return new JsonBuilder()
                            .startObject()
                            .addProperty("name", taskName)
                            .addProperty("type", "predicate")
                            .addRawProperty("parameters", parameters)
                            .endObject()
                            .toString();
                } else {
                    return new JsonBuilder()
                            .startObject()
                            .addProperty("name", atom.toString())
                            .addProperty("type", "predicate")
                            .addRawProperty("parameters", "[]")
                            .endObject()
                            .toString();
                }

            } else {
                return new JsonBuilder()
                        .startObject()
                        .addProperty("name", taskElement.getClass().getSimpleName())
                        .addProperty("type", "unknown")
                        .addProperty("value", taskElement.toString())
                        .addRawProperty("parameters", "[]")
                        .endObject()
                        .toString();
            }
        } catch (Exception e) {
            return new JsonBuilder()
                    .startObject()
                    .addProperty("type", "error")
                    .addProperty("message", e.getMessage())
                    .endObject()
                    .toString();
        }
    }

    private static String getTaskName(Predicate pred, InternalDomain domain) {
        try {
            int headIndex = pred.getHead();

            Vector<String> compoundTasks = domain.getCompoundTasks();
            if (headIndex >= 0 && headIndex < compoundTasks.size()) {
                String taskName = compoundTasks.get(headIndex);
                if (taskName.startsWith("!")) {
                    taskName = taskName.substring(1);
                }
                return taskName;
            }

            Vector<String> primitiveTasks = domain.getPrimitiveTasks();
            if (headIndex >= 0 && headIndex < primitiveTasks.size()) {
                String taskName = primitiveTasks.get(headIndex);
                if (taskName.startsWith("!")) {
                    taskName = taskName.substring(1);
                }
                return taskName;
            }

            return "task_" + headIndex;
        } catch (Exception e) {
            return "unknown_task";
        }
    }
}
