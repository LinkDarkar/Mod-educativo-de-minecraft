package net.linkdarkar.testmod.scripting;

public class ExpressionEvaluator
{
    public static Object evaluate(String expression, ExecutionContext ctx) {
        return new Object() {
            int pos = -1, ch;

            void nextChar()
            {
                ch = (++pos < expression.length()) ? expression.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            Object parse() {
                nextChar();
                Object x = parseExpression();
                if (pos < expression.length()) throw new RuntimeException("Unexpected: " + (char)ch);
                return x;
            }

            Object parseExpression() {
                Object x = parseTerm();
                for (;;) {
                    if (eat('+')) {
                        Object y = parseTerm();
                        // If either x or y is a String, treat + as concatenation
                        if (x instanceof String || y instanceof String) {
                            x = valToString(x) + valToString(y);
                        } else {
                            // Both are likely numbers
                            x = asDouble(x) + asDouble(y);
                        }
                    }
                    else if (eat('-')) {
                        Object y = parseTerm();
                        x = asDouble(x) - asDouble(y);
                    }
                    else return x;
                }
            }

            Object parseTerm() {
                Object x = parseFactor();
                for (;;) {
                    if      (eat('*')) x = asDouble(x) * asDouble(parseFactor()); // multiplication
                    else if (eat('/')) x = asDouble(x) / asDouble(parseFactor()); // division
                    else return x;
                }
            }

            Object parseFactor() {
                if (eat('+')) return parseFactor(); // unary plus
                if (eat('-')) return -asDouble(parseFactor()); // unary minus

                Object x;
                int startPos = pos;

                if (eat('('))
                {
                    x = parseExpression();
                    eat(')');
                }
                else if (eat('"'))
                {
                    StringBuilder sb = new StringBuilder();
                    while (ch != '"' && ch != -1)
                    {
                        sb.append((char)ch);
                        nextChar();
                    }
                    eat('"');
                    x = sb.toString();
                }
                else if (('0' <= ch && ch <= '9') || ch == '.')
                {
                    while (('0' <= ch && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(expression.substring(startPos, pos));
                }
                else if (('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || ch == '_')
                {
                    while (('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || ch == '_' || ('0' <= ch && ch <= '9')) nextChar();
                    String varName = expression.substring(startPos, pos);
                    x = ctx.GetVar(varName);
                }
                else
                {
                    throw new RuntimeException("Unexpected: " + (char)ch);
                }
                return x;
            }
            double asDouble(Object obj) {
                if (obj instanceof Number) return ((Number) obj).doubleValue();
                try {
                    return Double.parseDouble(obj.toString());
                } catch (Exception e) {
                    return 0; // Default or throw error
                }
            }

            String valToString(Object obj) {
                if (obj instanceof Double)
                {
                    double d = (Double) obj;
                    if (d == (long) d)
                    {
                        return String.format("%d", (long) d);
                    }
                }
                return obj.toString();
            }
        }.parse();
    }
}
