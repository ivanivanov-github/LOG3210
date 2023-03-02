package analyzer.visitors;

import analyzer.ast.*;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Vector;


/**
 * Ce visiteur explore l'AST et génère du code intermédiaire.
 *
 * @author Félix Brunet
 * @author Doriane Olewicki
 * @author Quentin Guidée
 * @version 2023.02.17
 */
public class IntermediateCodeGenVisitor implements ParserVisitor {
    private final PrintWriter m_writer;

    public HashMap<String, VarType> SymbolTable = new HashMap<>();

    private int id = 0;
    private int label = 0;

    public IntermediateCodeGenVisitor(PrintWriter writer) {
        m_writer = writer;
    }

    private String newID() {
        return "_t" + id++;
    }

    private String newLabel() {
        return "_L" + label++;
    }

    @Override
    public Object visit(SimpleNode node, Object data) {
        return data;
    }

    @Override
    public Object visit(ASTProgram node, Object data) {
        String S_next = newLabel();
        node.childrenAccept(this, S_next);
        // TODO
        m_writer.print(S_next + "\n");
        return null;
    }

    @Override
    public Object visit(ASTDeclaration node, Object data) {
        ASTIdentifier id = (ASTIdentifier) node.jjtGetChild(0);
        SymbolTable.put(id.getValue(), node.getValue().equals("bool") ? VarType.Bool : VarType.Number);
        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
//        node.childrenAccept(this, data);
        // TODO
        for (int i = 0; i < node.jjtGetNumChildren() - 1; i++) {
            String S_next = newLabel();
            node.jjtGetChild(i).jjtAccept(this, S_next);
            m_writer.println(S_next);
        }
        node.jjtGetChild(node.jjtGetNumChildren() - 1).jjtAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTScriptProgram node, Object data) {
        node.childrenAccept(this, data);
        // TODO
        return null;
    }

    @Override
    public Object visit(ASTScript node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTScriptCall node, Object data) {
        node.childrenAccept(this, data);
        // TODO
        return null;
    }

    @Override
    public Object visit(ASTStmt node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTIfStmt node, Object data) {
        node.childrenAccept(this, data);
        // TODO
        return null;
    }

    @Override
    public Object visit(ASTWhileStmt node, Object data) {
        node.childrenAccept(this, data);
        // TODO
        return null;
    }

    @Override
    public Object visit(ASTForStmt node, Object data) {
        node.childrenAccept(this, data);
        // TODO
        return null;
    }

    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        String identifier = ((ASTIdentifier) node.jjtGetChild(0)).getValue();
//        node.jjtGetChild(1).jjtAccept(this, data);
        // TODO
        String addr = node.jjtGetChild(1).jjtAccept(this, data).toString();
        m_writer.println(identifier + " = " + addr);
        return null;
    }

    @Override
    public Object visit(ASTExpr node, Object data) {
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    public Object codeExtAddMul(SimpleNode node, Object data, Vector<String> ops) {
        // À noter qu'il n'est pas nécessaire de boucler sur tous les enfants.
        // La grammaire n'accepte plus que 2 enfants maximum pour certaines opérations, au lieu de plusieurs
        // dans les TPs précédents. Vous pouvez vérifier au cas par cas dans le fichier Langage.jjt.
//        node.childrenAccept(this, data);
        // TODO
        if (ops.size() == 0) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        } else {
            String id = newID();

            String id1 = (String) node.jjtGetChild(0).jjtAccept(this, data);
            String id2 = (String) node.jjtGetChild(1).jjtAccept(this, data);

            m_writer.print(id + " = " + id1 + " " + ops.firstElement() + " " + id2 + "\n");
            return id;
        }
    }

    @Override
    public Object visit(ASTAddExpr node, Object data) {
        return codeExtAddMul(node, data, node.getOps());
    }

    @Override
    public Object visit(ASTMulExpr node, Object data) {
        return codeExtAddMul(node, data, node.getOps());
    }

    @Override
    public Object visit(ASTUnaExpr node, Object data) {
//        node.jjtGetChild(0).jjtAccept(this, data);
        // TODO
        if (node.getOps().isEmpty()) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        } else {
            String id = (node.jjtGetChild(0).jjtAccept(this, data)).toString();
            for (int i = 0; i < node.getOps().size(); i++) {
                String id1 = newID();

                m_writer.print(id1 + " = " + node.getOps().get(i) + " " + id + "\n");
                id = id1;
            }
            return id;
        }
    }

    @Override
    public Object visit(ASTBoolExpr node, Object data) {
//        node.childrenAccept(this, data);
        // TODO
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    public Object visit(ASTCompExpr node, Object data) {
//        node.childrenAccept(this, data);
        // TODO
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    public Object visit(ASTNotExpr node, Object data) {
//        node.jjtGetChild(0).jjtAccept(this, data);
        // TODO
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    public Object visit(ASTGenValue node, Object data) {
//        node.jjtGetChild(0).jjtAccept(this, data);
        // TODO
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    public Object visit(ASTBoolValue node, Object data) {
        // TODO
        return null;
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) {
        // TODO
        return node.getValue();
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        return Integer.toString(node.getValue());
    }

    public enum VarType {
        Bool,
        Number
    }

    private static class BoolLabel {
        public String lTrue;
        public String lFalse;

        public BoolLabel(String lTrue, String lFalse) {
            this.lTrue = lTrue;
            this.lFalse = lFalse;
        }
    }
}
