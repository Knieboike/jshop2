package JSHOP2;

/**
 * Converts LogicalExpressions to JSON format
 */
public class ExpressionJsonConverter {

    public static String expressionToJson(LogicalExpression expression, InternalDomain domain) {
        if (expression == null) {
            return "null";
        } else if (expression instanceof LogicalExpressionAtomic) {
            return createAtomicExpression((LogicalExpressionAtomic) expression, domain);
        } else if (expression instanceof LogicalExpressionConjunction) {
            return createConjunctionExpression((LogicalExpressionConjunction) expression, domain);
        } else if (expression instanceof LogicalExpressionDisjunction) {
            return createDisjunctionExpression((LogicalExpressionDisjunction) expression, domain);
        } else if (expression instanceof LogicalExpressionNegation) {
            return createNegationExpression((LogicalExpressionNegation) expression, domain);
        } else if (expression instanceof LogicalExpressionCall) {
            return createCallExpression((LogicalExpressionCall) expression, domain);
        } else if (expression instanceof LogicalExpressionAssignment) {
            return createAssignmentExpression((LogicalExpressionAssignment) expression, domain);
        } else if (expression instanceof LogicalExpressionForAll) {
            return createForallExpression((LogicalExpressionForAll) expression, domain);
        } else if (expression instanceof LogicalExpressionNil) {
            return createNilExpression();
        } else {
            return createUnknownExpression();
        }
    }

    private static String createAtomicExpression(LogicalExpressionAtomic atomic, InternalDomain domain) {
        Predicate predicate = atomic.logicalAtom;
        String predicateName = getPredicateName(predicate.getHead(), domain);
        String parameters = ParameterJsonConverter.parametersToJsonArray(predicate.getParam(), domain);

        return new JsonBuilder()
            .startObject()
            .addProperty("type", "atomic")
            .addProperty("predicate", predicateName)
            .addRawProperty("parameters", parameters)
            .endObject()
            .toString();
    }

    private static String createConjunctionExpression(LogicalExpressionConjunction conjunction, InternalDomain domain) {
        JsonBuilder builder = new JsonBuilder()
            .startObject()
            .addProperty("type", "and");

        builder.addRawProperty("conjuncts", createExpressionArray(conjunction.getExpression(), domain));

        return builder.endObject().toString();
    }

    private static String createDisjunctionExpression(LogicalExpressionDisjunction disjunction, InternalDomain domain) {
        JsonBuilder builder = new JsonBuilder()
            .startObject()
            .addProperty("type", "or");

        builder.addRawProperty("disjuncts", createExpressionArray(disjunction.getExpression(), domain));

        return builder.endObject().toString();
    }

    private static String createNegationExpression(LogicalExpressionNegation negation, InternalDomain domain) {
        return new JsonBuilder()
            .startObject()
            .addProperty("type", "not")
            .addRawProperty("expression", expressionToJson(negation.getExpression(), domain))
            .endObject()
            .toString();
    }

    private static String createExpressionArray(LogicalExpression[] expressions, InternalDomain domain) {
        JsonBuilder builder = new JsonBuilder().startArray();

        for (LogicalExpression expr : expressions) {
            builder.addRawArrayElement(expressionToJson(expr, domain));
        }

        return builder.endArray().toString();
    }

    private static String createCallExpression(LogicalExpressionCall call, InternalDomain domain) {
        // Zugriff auf das TermCall Objekt über Reflection oder public Getter
        try {
            java.lang.reflect.Field termField = LogicalExpressionCall.class.getDeclaredField("term");
            termField.setAccessible(true);
            TermCall termCall = (TermCall) termField.get(call);

            return new JsonBuilder()
                    .startObject()
                    .addProperty("type", "call")
                    .addProperty("function", termCall.toString())
                    .endObject()
                    .toString();
        } catch (Exception e) {
            return new JsonBuilder()
                    .startObject()
                    .addProperty("type", "call")
                    .addProperty("error", "Cannot access term field")
                    .endObject()
                    .toString();
        }
    }

    private static String createAssignmentExpression(LogicalExpressionAssignment assignment, InternalDomain domain) {

            return new JsonBuilder()
                    .startObject()
                    .addProperty("type", "assignment")
                    .addProperty("operation", assignment.toString())
                    .addProperty("function", assignment.getWhichVar())
                    .addProperty("value", assignment.getWhichVar())
                    .endObject()
                    .toString();

    }

    private static String createForallExpression(LogicalExpressionForAll forall, InternalDomain domain) {
        try {
            // Get the premise (condition) and consequence (result) of the forall expression
            LogicalExpression premise = forall.getPremise();
            LogicalExpression consequence = forall.getConsequence();

            // Convert premise to JSON (this is the "expression" part)
            String expressionJson = "null";
            if (premise != null) {
                expressionJson = expressionToJson(premise, domain);
            }

            // Convert consequence to JSON (this becomes the "predicates" part)
            String predicatesJson = "null";
            if (consequence != null) {
                predicatesJson = expressionToJson(consequence, domain);
            }

            // Variables extraction - for now use empty array
            // TODO: Extract actual variables from the forall expression
            String variablesJson = "[]";

            return new JsonBuilder()
                    .startObject()
                    .addProperty("type", "forall")
                    .addRawProperty("variables", variablesJson)
                    .addRawProperty("expression", expressionJson)
                    .addRawProperty("predicates", predicatesJson)
                    .addProperty("add_list", "true") // Default to true for now
                    .endObject()
                    .toString();
        } catch (Exception e) {
            return new JsonBuilder()
                    .startObject()
                    .addProperty("type", "forall")
                    .addProperty("error", "Failed to process forall expression: " + e.getMessage())
                    .addRawProperty("variables", "[]")
                    .addRawProperty("expression", "null")
                    .addRawProperty("predicates", "null")
                    .addProperty("add_list", "false")
                    .endObject()
                    .toString();
        }
    }

    private static String createNilExpression() {
        return "[]";
    }

    private static String createUnknownExpression() {
        return new JsonBuilder()
            .startObject()
            .addProperty("type", "unknown")
            .endObject()
            .toString();
    }

    private static String getPredicateName(int headIndex, InternalDomain domain) {
        if (domain != null && domain.getPrimitiveTasks() != null &&
            headIndex >= 0 && headIndex < domain.getPrimitiveTasks().size()) {
            return domain.getPrimitiveTasks().get(headIndex);
        }
        return "pred" + headIndex;
    }
}
