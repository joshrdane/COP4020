package plc.project;

import java.io.PrintWriter;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        print("public class Main {");
        newline(indent++);
        ast.getFields().forEach(field -> {
            newline(indent);
            visit(field);
        });
        newline(indent);
        print("public static void main(String[] args) {");
        newline(indent);
        print("    System.exit(new Main().main());");
        newline(indent);
        print("}");
        newline(0);
        ast.getMethods().forEach(method -> {
            newline(indent);
            visit(method);
        });
        newline(--indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        print(ast.getTypeName(), " ", ast.getVariable().getJvmName());
        ast.getValue().ifPresent(value -> print(" = ", visit(value)));
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        print(Environment.getType(ast.getReturnTypeName().orElseThrow(RuntimeException::new)).getJvmName(), " ", ast.getName(), "(");
        for (int i = 0; i < ast.getParameters().size(); i++) {
            if (i > 0) {
                print(", ");
            }
            print(ast.getParameterTypeNames().get(i), " ", ast.getParameters().get(i));
        }
        print(") {");
        indent++;
        ast.getStatements().forEach(statement -> {
            newline(indent);
            visit(statement);
        });
        newline(--indent);
        print("}");
        newline(0);
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        print(ast.getExpression(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        print(ast.getVariable().getType().getJvmName(), " ", ast.getVariable().getJvmName());
        ast.getValue().ifPresent(value -> print(" = ", value));
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        print(visit(ast.getReceiver()), " = ", visit(ast.getValue()), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        print("if (", ast.getCondition(), ") {");
        indent++;
        ast.getThenStatements().forEach(statement -> {
            newline(indent);
            visit(statement);
        });
        newline(--indent);
        if (!ast.getElseStatements().isEmpty()) {
            print("} else {");
            indent++;
            ast.getElseStatements().forEach(statement -> {
                newline(indent);
                visit(statement);
            });
            indent--;
            newline(indent);
        }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        print("for (int : ", ast.getName(), ") {");
        indent++;
        ast.getStatements().forEach(statement -> {
            newline(indent);
            visit(statement);
        });
        newline(--indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        print("while (", ast.getCondition(), ") {");
        if (!ast.getStatements().isEmpty()) {
            indent++;
            for (int i = 0; i < ast.getStatements().size(); i++) {
                newline(indent);
                print(ast.getStatements().get(i));
            }
            newline(--indent);
        }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        print("return ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        String result = ast.getLiteral().toString();
        if (ast.getLiteral() instanceof Character) {
            result = String.format("'%s'", result);
        } else if (ast.getLiteral() instanceof String) {
            result = String.format("\"%s\"", result);
        }
        print(result);
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        print("(", visit(ast.getExpression()), ")");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        String operator;
        switch (ast.getOperator()) {
            default:
                operator = ast.getOperator();
                break;
            case "AND":
                operator = "&&";
                break;
            case "OR":
                operator = "||";
                break;
        }
        print(ast.getLeft(), " ", operator, " ", ast.getRight());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        ast.getReceiver().ifPresent(receiver -> print(receiver, "."));
        print(ast.getVariable().getJvmName());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        ast.getReceiver().ifPresent(receiver -> print(receiver, "."));
        print(ast.getFunction().getJvmName(), "(");
        for (int i = 0; i < ast.getArguments().size(); i++) {
            if (i > 0) {
                print(", ");
            }
            print(ast.getArguments().get(i));
        }
        print(")");
        return null;
    }

}
