package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Method method;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        if (scope.lookupFunction("main", 0).getReturnType() != Environment.Type.INTEGER) {
            throw new RuntimeException("Main function must return an Integer.");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        requireAssignable(Environment.getType(ast.getTypeName()), ast.getValue().orElseThrow(RuntimeException::new).getType());
        visit(ast.getValue().get());
        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), ast.getValue().get().getType(), Environment.NIL));
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        if (!(ast.getExpression() instanceof Ast.Expr.Function)) {
            throw new RuntimeException("Expression must be a function type");
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        Optional<String> optTypeName = ast.getTypeName();
        Optional<Ast.Expr> optValue = ast.getValue();

        if (!optTypeName.isPresent() && !optValue.isPresent()) {
            throw new RuntimeException("Declaration must have type or value to infer type");
        }

        Environment.Type type = null;

        if (ast.getTypeName().isPresent()) {
            type = Environment.getType(ast.getTypeName().get());
        }

        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());

            if (type == null) {
                type = ast.getValue().get().getType();
            }

            requireAssignable(type, ast.getValue().get().getType());
        }

        ast.setVariable(scope.defineVariable(ast.getName(), ast.getName(), type, Environment.NIL));

        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        visit(ast.getCondition());
        if (ast.getCondition().getType() != Environment.Type.BOOLEAN) {
            throw new RuntimeException("Condition must evaluate to a Boolean");
        } else if (ast.getThenStatements().size() == 0) {
            throw new RuntimeException("There must be at least one Then Statement");
        }
        for (Ast.Stmt stmt : ast.getThenStatements()) {
            try {
                scope = new Scope(scope);
                visit(stmt);
            } finally {
                scope = scope.getParent();
            }
        }
        for (Ast.Stmt stmt : ast.getElseStatements()) {
            try {
                scope = new Scope(scope);
                visit(stmt);
            } finally {
                scope = scope.getParent();
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        try {
            scope = new Scope(scope);
            for (Ast.Stmt stmt : ast.getStatements()) {
                visit(stmt);
            }
        } finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        if (ast.getLiteral() == Environment.NIL) {
            ast.setType(Environment.Type.NIL);
        } else if (ast.getLiteral() instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        } else if (ast.getLiteral() instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        } else if (ast.getLiteral() instanceof String) {
            ast.setType(Environment.Type.STRING);
        } else if (ast.getLiteral() instanceof BigInteger) {
            if (
                    ((BigInteger) ast.getLiteral()).compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0 ||
                    ((BigInteger) ast.getLiteral()).compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0
            ) {
                throw new RuntimeException("Integer not in range");
            }
            ast.setType(Environment.Type.INTEGER);
        } else if (ast.getLiteral() instanceof BigDecimal) {
            if (!ast.getLiteral().equals(BigDecimal.valueOf(((BigDecimal) ast.getLiteral()).doubleValue()))) {
                throw new RuntimeException("Double not in range");
            }
            ast.setType(Environment.Type.DECIMAL);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        visit(ast.getLeft());
        visit(ast.getRight());
        if (
                Arrays.stream(new String[] {"AND", "OR"})
                        .anyMatch(operator -> operator.equals(ast.getOperator()))
        ) {
            if (
                    Arrays.stream(new Ast.Expr[] {ast.getLeft(), ast.getRight()})
                            .map(Ast.Expr::getType)
                            .anyMatch(type -> type != Environment.Type.BOOLEAN)
            ) {
                throw new RuntimeException("Both operands must be of type Boolean");
            }
            ast.setType(Environment.Type.BOOLEAN);
        } else if (
                Arrays.stream(new String[] {"<", "<=", ">", ">=", "==", "!="})
                        .anyMatch(operator -> operator.equals(ast.getOperator()))
        ) {
            if (
                    Arrays.stream(new Ast.Expr[] {ast.getLeft(), ast.getRight()})
                            .map(Ast.Expr::getType)
                            .map(Environment.Type::getScope)
                            .anyMatch(scopeType -> scopeType != Environment.Type.COMPARABLE.getScope())
            ) {
                throw new RuntimeException("Both operands must be Comparable");
            } else if (
                    Arrays.stream(new Ast.Expr[] {ast.getLeft(), ast.getRight()})
                            .map(Ast.Expr::getType)
                            .distinct()
                            .count() != 1
            ) {
                throw new RuntimeException("Both operands must be of the same type");
            }
            ast.setType(Environment.Type.BOOLEAN);
        } else if (ast.getOperator().equals("+")) {
            if (
                    Arrays.stream(new Ast.Expr[] {ast.getLeft(), ast.getRight()})
                            .map(Ast.Expr::getType)
                            .anyMatch(expr -> expr == Environment.Type.STRING)
            ) {
                ast.setType(Environment.Type.STRING);
            } else {
                checkMatchingComparable(ast);
            }
        } else if (
                Arrays.stream(new String[] {"-", "*", "/"})
                        .anyMatch(operator -> operator.equals(ast.getOperator()))
        ) {
            checkMatchingComparable(ast);
        }
        return null;
    }

    private static void checkMatchingComparable(Ast.Expr.Binary ast) {
        if (
                Arrays.stream(new Ast.Expr[] {ast.getLeft(), ast.getRight()})
                        .map(Ast.Expr::getType)
                        .anyMatch(type -> type != Environment.Type.INTEGER && type != Environment.Type.DECIMAL)
        ) {
            throw new RuntimeException("Both operands must be an Integer or Decimal");
        } else if (
                Arrays.stream(new Ast.Expr[] {ast.getLeft(), ast.getRight()})
                        .map(Ast.Expr::getType)
                        .distinct()
                        .count() != 1
        ) {
            throw new RuntimeException("Both operands must be of the same type");
        } else {
            ast.setType(ast.getLeft().getType());
        }
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        // TODO fix this
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
        }
        ast.setVariable(scope.lookupVariable(ast.getName()));
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        // TODO complete this
        ast.getReceiver().ifPresent(this::visit);
        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getArguments().size()));
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if ((
                target != type
        ) && (
                target != Environment.Type.ANY
        ) && (
                target != Environment.Type.COMPARABLE || (
                        type != Environment.Type.INTEGER &&
                        type != Environment.Type.DECIMAL &&
                        type != Environment.Type.CHARACTER &&
                        type != Environment.Type.STRING
                )
        )) {
            throw new RuntimeException("Target type does not match the type being used or assigned.");
        }
    }

}
