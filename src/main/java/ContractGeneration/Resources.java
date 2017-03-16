package ContractGeneration;

import com.github.javaparser.ast.expr.*;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by meddan on 16/02/17.
 */
public class Resources {

    private static ArrayList<Class> ignorableExpressions = new ArrayList<>();
    private static ArrayList<Class> ignorableReplaceExpressions = new ArrayList<>();
    public static boolean ignorableExpression(Expression e){
        if(ignorableExpressions.isEmpty()){
            addIgnorableExpressions();
        }
        return ignorableExpressions.contains(e.getClass());
    }

    private static void addIgnorableExpressions() {
        ignorableExpressions.add(FieldAccessExpr.class);
        ignorableExpressions.add(IntegerLiteralExpr.class);
        ignorableExpressions.add(DoubleLiteralExpr.class);
        ignorableExpressions.add(LongLiteralExpr.class);
        ignorableExpressions.add(StringLiteralExpr.class);
        ignorableExpressions.add(BooleanLiteralExpr.class);
        ignorableExpressions.add(CharLiteralExpr.class);
        ignorableExpressions.add(NullLiteralExpr.class);
        ignorableExpressions.add(ClassExpr.class);
        ignorableExpressions.add(AnnotationExpr.class);
        ignorableExpressions.add(TypeExpr.class);
        ignorableExpressions.add(ThisExpr.class);
    }
    private static void addIgnorableReplaceExpressions(){
        ignorableReplaceExpressions.add(ObjectCreationExpr.class);
        ignorableReplaceExpressions.add(InstanceOfExpr.class);
        ignorableReplaceExpressions.add(LambdaExpr.class);
        ignorableReplaceExpressions.add(SuperExpr.class);
        ignorableReplaceExpressions.add(NullLiteralExpr.class);
        ignorableReplaceExpressions.add(ClassExpr.class);
        ignorableReplaceExpressions.add(AnnotationExpr.class);
        ignorableReplaceExpressions.add(TypeExpr.class);
        ignorableReplaceExpressions.add(IntegerLiteralExpr.class);
        ignorableReplaceExpressions.add(DoubleLiteralExpr.class);
        ignorableReplaceExpressions.add(LongLiteralExpr.class);
        ignorableReplaceExpressions.add(StringLiteralExpr.class);
        ignorableReplaceExpressions.add(BooleanLiteralExpr.class);
        ignorableReplaceExpressions.add(CharLiteralExpr.class);

    }

    public static boolean ignorableReplaceExpression(Expression e) {
        if(ignorableReplaceExpressions.isEmpty()){
            addIgnorableReplaceExpressions();
        }
        return ignorableReplaceExpressions.contains(e.getClass());
    }
    public static String removeOld(String name){
        Matcher m = Pattern.compile("\\\\old\\(([a-zA-Z._]+)\\)").matcher(name);
        StringBuffer sb = new StringBuffer();
        while (m.find()){
            m.appendReplacement(sb,"$1");
        }
        m.appendTail(sb);
        return sb.toString();
    }

}
