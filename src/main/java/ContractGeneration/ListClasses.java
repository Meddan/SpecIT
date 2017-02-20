package ContractGeneration;

import ContractGeneration.DirExplorer;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.google.common.base.Strings;

import java.io.File;
import java.io.IOException;

public class ListClasses {

    public static void listClasses(File projectDir) {
        new DirExplorer((level, path, file) -> path.endsWith(".java"), (level, path, file) -> {
            System.out.println(path);
            System.out.println(Strings.repeat("=", path.length()));
            try {
                new VoidVisitorAdapter<Object>() {
                    @Override
                    public void visit(ClassOrInterfaceDeclaration n, Object arg) {
                        super.visit(n, arg);
                        System.out.println(" * " + n.getName());
                        System.out.println("members: ");
                        for(BodyDeclaration<?> b: n.getMembers()){
                            if(b instanceof FieldDeclaration){
                                System.out.println(b.toString());
                            }
                            /*System.out.println(b.toString());
                            System.out.println();
                            System.out.println(b.getComment());
                            System.out.println();
                            */
                        }
                    }
                }.visit(JavaParser.parse(file), null);
                System.out.println(); // empty line
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).explore(projectDir);
    }

    public static void main(String[] args) {
        File projectDir = new File("src/main/java");
        listClasses(projectDir);
    }
}