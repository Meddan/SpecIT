package ContractGeneration;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

public class RemoveOldVisitor extends ModifierVisitor<Void> {
    @Override
    public Visitable visit(NameExpr ne, Void args) {
        String newName = Resources.removeOld(ne.getName().toString());
        ne.getName().setIdentifier(newName);
        return ne;
    }
    public Visitable visit(Expression e, Void args) {
        if( e instanceof AnnotationExpr){
            return visit((AnnotationExpr) e, args);
        }
        if( e instanceof ArrayAccessExpr){
            return visit((ArrayAccessExpr) e, args);
        }
        if( e instanceof ArrayCreationExpr){
            return visit((ArrayCreationExpr) e, args);
        }
        if( e instanceof ArrayInitializerExpr){
            return visit((ArrayInitializerExpr) e, args);
        }
        if( e instanceof AssignExpr){
            return visit((AssignExpr) e, args);
        }
        if( e instanceof BinaryExpr){
            return visit((BinaryExpr) e, args);
        }
        if( e instanceof BooleanLiteralExpr){
            return visit((BooleanLiteralExpr) e, args);
        }
        if( e instanceof CastExpr){
            return visit((CastExpr) e, args);
        }
        if( e instanceof CharLiteralExpr){
            return visit((CharLiteralExpr) e, args);
        }
        if( e instanceof ClassExpr){
            return visit((ClassExpr) e, args);
        }
        if( e instanceof ConditionalExpr){
            return visit((ConditionalExpr) e, args);
        }
        if( e instanceof DoubleLiteralExpr){
            return visit((DoubleLiteralExpr) e, args);
        }
        if( e instanceof EnclosedExpr){
            return visit((EnclosedExpr) e, args);
        }
        if( e instanceof FieldAccessExpr){
            return visit((FieldAccessExpr) e, args);
        }
        if( e instanceof InstanceOfExpr){
            return visit((InstanceOfExpr) e, args);
        }
        if( e instanceof IntegerLiteralExpr){
            return visit((IntegerLiteralExpr) e, args);
        }
        if( e instanceof LambdaExpr){
            return visit((LambdaExpr) e, args);
        }
        if( e instanceof LiteralExpr){
            System.out.println(e);
            return null;
        }
        if( e instanceof MethodCallExpr){
            return visit((MethodCallExpr) e, args);
        }
        if( e instanceof MethodReferenceExpr){
            return visit((MethodReferenceExpr) e, args);
        }
        if( e instanceof NameExpr){
            return visit((NameExpr) e, args);
        }
        if( e instanceof ObjectCreationExpr){
            return visit((ObjectCreationExpr) e, args);
        }
        if( e instanceof SuperExpr){
            return visit((SuperExpr) e, args);
        }
        if( e instanceof ThisExpr){
            return visit((ThisExpr) e, args);
        }
        if( e instanceof TypeExpr){
            return visit((TypeExpr) e, args);
        }
        if( e instanceof UnaryExpr){
            return visit((UnaryExpr) e, args);
        }
        if( e instanceof VariableDeclarationExpr){
            return visit((VariableDeclarationExpr) e, args);
        }
        return null;
    }
}