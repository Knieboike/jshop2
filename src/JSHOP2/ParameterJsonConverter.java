package JSHOP2;

/**
 * Converts JSHOP2 parameters to JSON format
 */
public class ParameterJsonConverter {

    public static String paramToJson(Term param, InternalDomain domain) {
        if (param == null) {
            return createUnknownParam();
        }

        if (param instanceof TermVariable) {
            TermVariable var = (TermVariable) param;
            return new JsonBuilder()
                    .startObject()
                    .addProperty("name", getVariableName(var, domain)) // Korrekte Extraktion des Variablennamens
                    .addProperty("type", "Variable")
                    .endObject()
                    .toString();
        }

        if (param instanceof TermConstant) {
            return createConstantParam((TermConstant) param, domain);
        }

        if (param instanceof TermNumber) {
            return createNumberParam((TermNumber) param);
        }

        if (param instanceof TermList) {
            return createListParam((TermList) param, domain);
        }

        return createUnknownParam();
    }

    public static String parametersToJsonArray(Term param, InternalDomain domain) {
        JsonBuilder builder = new JsonBuilder();
        builder.startArray();

        if (param != null && !param.isNil()) {
            if (param instanceof TermList) {
                addListParameters(builder, ((TermList) param).getList(), domain);
            } else {
                builder.addRawArrayElement(paramToJson(param, domain));
            }
        }

        return builder.endArray().toString();
    }

    private static void addListParameters(JsonBuilder builder, List paramList, InternalDomain domain) {
        while (paramList != null && paramList.getHead() != null) {
            builder.addRawArrayElement(paramToJson(paramList.getHead(), domain));
            paramList = (paramList.getTail() instanceof TermList)
                ? ((TermList) paramList.getTail()).getList()
                : null;
        }
    }

    private static String createConstantParam(TermConstant constant, InternalDomain domain) {
        int idx = constant.getIndex();
        String name = getConstantName(idx, domain);

        return new JsonBuilder()
            .startObject()
            .addProperty("name", name)
            .addProperty("type", "constant")
            .endObject()
            .toString();
    }

    private static String createNumberParam(TermNumber number) {
        return new JsonBuilder()
            .startObject()
            .addProperty("name", String.valueOf(number.getNumber()))
            .addProperty("type", "number")
            .endObject()
            .toString();
    }

    private static String createListParam(TermList list, InternalDomain domain) {
        JsonBuilder builder = new JsonBuilder().startArray();
        List l = list.getList();

        while (l != null && l.getHead() != null) {
            builder.addRawArrayElement(paramToJson(l.getHead(), domain));
            l = (l.getTail() instanceof TermList) ? ((TermList) l.getTail()).getList() : null;
        }

        return builder.endArray().toString();
    }

    private static String createUnknownParam() {
        return new JsonBuilder()
            .startObject()
            .addProperty("name", "unknown")
            .addProperty("type", "unknown")
            .endObject()
            .toString();
    }

    private static String getConstantName(int idx, InternalDomain domain) {
        if (domain != null && domain.getConstants() != null && idx < domain.getConstants().size()) {
            return domain.getConstants().get(idx);
        }
        return "const" + idx;
    }

    private static String getVariableName(TermVariable variable, InternalDomain domain) {
        try {
            // Use the getVariableName method from InternalDomain instead of getVariableNames().get()
            String varName = domain.getVariableName(variable.getIndex());
            // If the variable name doesn't start with '?', add it to match the domain syntax
            if (varName != null && !varName.startsWith("?") && !varName.startsWith("var")) {
                return varName;
            }
            return varName != null ? varName : variable.toString();
        } catch (Exception e) {
            return variable.toString();
        }
    }
}
