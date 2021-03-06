package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        ast.getFields().forEach(this::visit);
        ast.getMethods().forEach(this::visit);
        return scope.lookupFunction("main", 0).invoke(null);
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        scope.defineVariable(ast.getName(), ast.getValue().isPresent() ? visit(ast.getValue().get()) : Environment.NIL);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        Scope definition = scope;
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            Environment.PlcObject result = Environment.NIL;
            try {
                scope = new Scope(definition);
                for (int i = 0; i < ast.getParameters().size(); i++) {
                    scope.defineVariable(ast.getParameters().get(i), args.get(i));
                }
                ast.getStatements().forEach(this::visit);
            } catch (Return exception) {
                result = exception.value;
            } finally {
                scope = scope.getParent();
            }
            return result;
        });
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Declaration ast) {
        scope.defineVariable(ast.getName(), ast.getValue().isPresent() ? visit(ast.getValue().get()) : Environment.NIL);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment ast) {
        Ast.Expr.Access access = (Ast.Expr.Access) ast.getReceiver();
        Environment.PlcObject value = visit(ast.getValue());
        if (access.getReceiver().isPresent()) {
            visit(access.getReceiver().get()).setField(access.getName(), value);
        } else {
            scope.lookupVariable(access.getName()).setValue(value);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {
        try {
            scope = new Scope(scope);
            if (requireType(Boolean.class, visit(ast.getCondition()))) {
                ast.getThenStatements().forEach(this::visit);
            } else {
                ast.getElseStatements().forEach(this::visit);
            }
        } finally {
            scope = scope.getParent();
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {
        requireType(Iterable.class, visit(ast.getValue())).forEach(driver -> {
            try {
                scope = new Scope(scope);
                scope.defineVariable(ast.getName(), (Environment.PlcObject) driver);
                ast.getStatements().forEach(this::visit);
            } finally {
                scope = scope.getParent();
            }
        });
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                ast.getStatements().forEach(this::visit);
            } finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal ast) {
        return ast.getLiteral() == null ? Environment.NIL : Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast) {
        Environment.PlcObject left = visit(ast.getLeft());
        if (ast.getOperator().equals("OR")) {
            return Environment.create(requireType(Boolean.class, left) || requireType(Boolean.class, visit(ast.getRight())));
        }
        Environment.PlcObject right  = visit(ast.getRight());
        switch (ast.getOperator()) {
            case "AND":
                return Environment.create(requireType(Boolean.class, left) && requireType(Boolean.class, right));
            case "<":
                if (left.getValue().getClass() == right.getValue().getClass()) {
                    return Environment.create(requireType(Comparable.class, left).compareTo(requireType(Comparable.class, right)) < 0);
                }
                break;
            case "<=":
                if (left.getValue().getClass() == right.getValue().getClass()) {
                    return Environment.create(requireType(Comparable.class, left).compareTo(requireType(Comparable.class, right)) <= 0);
                }
                break;
            case ">":
                if (left.getValue().getClass() == right.getValue().getClass()) {
                    return Environment.create(requireType(Comparable.class, left).compareTo(requireType(Comparable.class, right)) > 0);
                }
            case ">=":
                if (left.getValue().getClass() == right.getValue().getClass()) {
                    return Environment.create(requireType(Comparable.class, left).compareTo(requireType(Comparable.class, right)) >= 0);
                }
                break;
            case "==":
                return Environment.create(left.getValue().equals(right.getValue()));
            case "!=":
                return Environment.create(!left.getValue().equals(right.getValue()));
            case "+":
                if (left.getValue() instanceof String || right.getValue() instanceof String) {
                    return Environment.create(left.getValue().toString() + right.getValue().toString());
                }
                try {
                    return Environment.create(requireType(BigInteger.class, left).add(requireType(BigInteger.class, right)));
                } catch (RuntimeException ignored) {}
                try {
                    return Environment.create(requireType(BigDecimal.class, left).add(requireType(BigDecimal.class, right)));
                } catch (RuntimeException ignored) {}
                break;
            case "-":
                try {
                    return Environment.create(requireType(BigInteger.class, left).subtract(requireType(BigInteger.class, right)));
                } catch (RuntimeException ignored) {}
                try {
                    return Environment.create(requireType(BigDecimal.class, left).subtract(requireType(BigDecimal.class, right)));
                } catch (RuntimeException ignored) {}
                break;
            case "*":
                try {
                    return Environment.create(requireType(BigInteger.class, left).multiply(requireType(BigInteger.class, right)));
                } catch (RuntimeException ignored) {}
                try {
                    return Environment.create(requireType(BigDecimal.class, left).multiply(requireType(BigDecimal.class, right)));
                } catch (RuntimeException ignored) {}
                break;
            case "/":
                try {
                    return Environment.create(requireType(BigInteger.class, left).divide(requireType(BigInteger.class, right)));
                } catch (RuntimeException ignored) {}
                try {
                    return Environment.create(requireType(BigDecimal.class, left).divide(requireType(BigDecimal.class, right), RoundingMode.HALF_EVEN));
                } catch (RuntimeException ignored) {}
                break;
        }
        throw new RuntimeException();
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {
        return (ast.getReceiver().isPresent() ? visit(ast.getReceiver().get()).getField(ast.getName()) : scope.lookupVariable(ast.getName())).getValue();
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {
        List<Environment.PlcObject> arguments = new ArrayList<>();
        ast.getArguments().forEach((argument) -> arguments.add(visit(argument)));
        if (ast.getReceiver().isPresent()) {
            return visit(ast.getReceiver().get()).callMethod(ast.getName(), arguments);
        } else {
            return scope.lookupFunction(ast.getName(), ast.getArguments().size()).invoke(arguments);
        }
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
