package plc.project;

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
        scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        Environment.PlcObject result = Environment.NIL;
        try {
            scope = new Scope(scope);
            ast.getParameters().forEach((name) -> scope.defineVariable(name, Environment.NIL));
            ast.getStatements().forEach(this::visit);
        } catch (Return exception) {
            result = exception.value;
        } finally {
            scope = scope.getParent();
        }
        return result;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Declaration ast) {
        scope.defineVariable(ast.getName(), ast.getValue().isPresent() ? visit(ast.getValue().get()) : Environment.NIL);
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment ast) {
        throw new UnsupportedOperationException(); //TODO
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
        requireType(Iterable.class, visit(ast.getValue())).forEach((plcObject) -> {
            try {
                scope = new Scope(scope);
                scope.defineVariable(ast.getName(), Environment.NIL); // Initialize the variable as NIL ???
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
        return new Environment.PlcObject(scope, ast.getLiteral() == null ? Environment.NIL.getValue() : ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Group ast) {
        return visit(ast.getExpression());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast) {
        switch (ast.getOperator()) {
            case "AND":
                return Environment.create(requireType(Boolean.class, visit(ast.getLeft())) && requireType(Boolean.class, visit(ast.getRight())));
            case "OR":
                return Environment.create(requireType(Boolean.class, visit(ast.getLeft())) || requireType(Boolean.class, visit(ast.getRight())));
            case "<":
                return Environment.create(requireType(Comparable.class, visit(ast.getLeft())).compareTo(requireType(Comparable.class, visit(ast.getRight()))) < 0);
            case "<=":
                return Environment.create(requireType(Comparable.class, visit(ast.getLeft())).compareTo(requireType(Comparable.class, visit(ast.getRight()))) <= 0);
            case ">":
                return Environment.create(requireType(Comparable.class, visit(ast.getLeft())).compareTo(requireType(Comparable.class, visit(ast.getRight()))) > 0);
            case ">=":
                return Environment.create(requireType(Comparable.class, visit(ast.getLeft())).compareTo(requireType(Comparable.class, visit(ast.getRight()))) >= 0);
        }
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {
        return ast.getReceiver().isPresent() ? visit(ast.getReceiver().get()).getField(ast.getName()).getValue() : scope.lookupVariable(ast.getName()).getValue();
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {
        throw new UnsupportedOperationException(); //TODO
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
