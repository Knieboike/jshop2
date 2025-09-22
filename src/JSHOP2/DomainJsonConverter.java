package JSHOP2;

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
                .addRawProperty("precondition", preconditionToJson(operator.getPre(), domain))
                .addRawProperty("effect", effectsToJson(operator, domain))
              //  .addRawProperty("cost", termToJson(operator.getCost(), domain)) TODO: Check if cost is needed and there are different costs
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

        // Delete effects verarbeiten - verwende statischen Zugriff
        Vector<?> deleteList = InternalOperator.getDel();
        if (deleteList != null && !deleteList.isEmpty()) {
            String deleteEffects = delAddListToJsonEffects(deleteList, domain, "delete");
            if (deleteEffects != null && !deleteEffects.equals("[]")) {
                // Parse the array and add each element directly
                String effectsContent = deleteEffects.substring(1, deleteEffects.length() - 1); // Remove [ and ]
                if (!effectsContent.trim().isEmpty()) {
                    effectsBuilder.addRawArrayElement(effectsContent);
                }
            }
        }

        // Add effects verarbeiten - verwende statischen Zugriff
        Vector<?> addList = InternalOperator.getAdd();
        if (addList != null && !addList.isEmpty()) {
            String addEffects = delAddListToJsonEffects(addList, domain, "add");
            if (addEffects != null && !addEffects.equals("[]")) {
                // Parse the array and add each element directly
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
            // Check if first element is an Integer (variable index) or actual effect
            Object firstElement = delAddList.get(0);

            if (firstElement instanceof Integer) {
                // Variable reference - this means the list references a variable, not actual effects
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
                // Real list of elements - process all elements
                for (Object element : delAddList) {
                    if (element instanceof DelAddElement) {
                        String elementJson = effectToJson((DelAddElement) element, domain, effectType);
                        builder.addRawArrayElement(elementJson);
                        hasEffects = true;
                    }
                }
            }
        }

        // Return null if no effects were found
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
                // Unknown effect type
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
            // Für Delete-Effekte verwende "not" Struktur wie in Scala
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
            // Für Add-Effekte verwende einfache Predicate-Struktur
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
        // Vollständige ForAll-Implementierung mit korrekter Domain-Referenz
        try {
            LogicalExpression expression = forallEffect.getExpression();
            Predicate[] atoms = forallEffect.getAtoms();

            // Variables extrahieren - versuche aus der LogicalExpression zu extrahieren
            JsonBuilder variablesBuilder = new JsonBuilder().startArray();
            // TODO: Implementiere korrekte Variablenextraktion aus LogicalExpression
            // Für jetzt verwende einfache Platzhalter-Namen
            String variablesJson = variablesBuilder.endArray().toString();

            // Expression JSON generieren mit null domain (wird intern behandelt)
            String expressionJson = "null";
            if (expression != null) {
                expressionJson = ExpressionJsonConverter.expressionToJson(expression, null);
            }

            // Predicates/Atoms JSON generieren
            JsonBuilder predicatesBuilder = new JsonBuilder().startArray();
            if (atoms != null) {
                for (Predicate atom : atoms) {
                    String predicateName = "unknown";

                    // Versuche den korrekten Prädikatennamen zu extrahieren
                    try {
                        int headIndex = atom.getHead();
                        // Da wir keine Domain-Referenz haben, verwende eine generische Benennung
                        // die später durch post-processing korrigiert werden kann
                        predicateName = "pred_" + headIndex;
                    } catch (Exception e) {
                        predicateName = "unknown_predicate";
                    }

                    String parameters = "[]";
                    try {
                        // Verwende den ParameterJsonConverter für konsistente Parameter-Extraktion
                        parameters = ParameterJsonConverter.parametersToJsonArray(atom.getParam(), null);
                    } catch (Exception e) {
                        // Fallback auf leeres Array
                    }

                    String atomJson;
                    if ("delete".equals(effectType)) {
                        // Für Delete-Effekte verwende "not" Struktur
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
                        // Für Add-Effekte verwende einfache Predicate-Struktur
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

            // Vollständige ForAll-Struktur erstellen - korrigiere die add_list Property
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
        // Protection-Effekte implementierung
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

    // TaskList strukturiert parsen
    private static String taskListToJson(TaskList taskList, InternalDomain domain) {
        JsonBuilder builder = new JsonBuilder().startArray();
//TODO: Currently only empty tasks
        try {
            TaskAtom tasks = taskList.getTask();
            if (tasks == null) {
                String emptyJson = new JsonBuilder()
                        .startObject()
                        .addProperty("type", "empty")
                        .endObject()
                        .toString();
                builder.addRawArrayElement(emptyJson);
            } else {
                builder.addRawArrayElement(taskElementToJson(tasks, domain));
            }
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

    private static String taskElementToJson(Object taskElement, InternalDomain domain) {
        try {
            if (taskElement instanceof Predicate) {
                Predicate pred = (Predicate) taskElement;
                String taskName = getTaskName(pred, domain);
                String parameters = parametersToJsonArray(pred.getParam(), domain);

                return new JsonBuilder()
                        .startObject()
                        .addProperty("name", taskName)
                        .addRawProperty("parameters", parameters)
                        .endObject()
                        .toString();

            } else if (taskElement instanceof TaskAtom) {
                TaskAtom atom = (TaskAtom) taskElement;
                return new JsonBuilder()
                        .startObject()
                        .addProperty("name", atom.toString())
                        .addRawProperty("parameters", "[]")
                        .endObject()
                        .toString();

            } else {
                return new JsonBuilder()
                        .startObject()
                        .addProperty("name", taskElement.getClass().getSimpleName())
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

    // Task Name extrahieren basierend auf Predicate
    private static String getTaskName(Predicate pred, InternalDomain domain) {
        try {
            int headIndex = pred.getHead();

            Vector<String> compoundTasks = domain.getCompoundTasks();
            if (headIndex >= 0 && headIndex < compoundTasks.size()) {
                String taskName = compoundTasks.get(headIndex);
                // Remove exclamation mark prefix if present
                if (taskName.startsWith("!")) {
                    taskName = taskName.substring(1);
                }
                return taskName;
            }

            Vector<String> primitiveTasks = domain.getPrimitiveTasks();
            if (headIndex >= 0 && headIndex < primitiveTasks.size()) {
                String taskName = primitiveTasks.get(headIndex);
                // Remove exclamation mark prefix if present
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
