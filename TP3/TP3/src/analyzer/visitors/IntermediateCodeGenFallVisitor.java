package analyzer.visitors;

import analyzer.ast.*;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
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
public class IntermediateCodeGenFallVisitor implements ParserVisitor {
    public static final String FALL = "fall";

    private final PrintWriter m_writer;

    public HashMap<String, IntermediateCodeGenVisitor.VarType> SymbolTable = new HashMap<>();

    private int id = 0;
    private int label = 0;

    public IntermediateCodeGenFallVisitor(PrintWriter writer) {
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
        SymbolTable.put(id.getValue(), node.getValue().equals("bool") ? IntermediateCodeGenVisitor.VarType.Bool : IntermediateCodeGenVisitor.VarType.Number);
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
//        node.childrenAccept(this, data);
        // TODO
        ArrayList<String> scriptCalls = new ArrayList<String>((Collection<String>) node
                .jjtGetChild(node.jjtGetNumChildren() - 1)
                .jjtAccept(this, data));

        for (int i = 0; i < scriptCalls.size(); i++) {
            for (int j = 0; j < node.jjtGetNumChildren() - 1; j++) {
                if (scriptCalls.get(i).equals(((ASTScript) node.jjtGetChild(j)).getValue())) {
                    node.jjtGetChild(j).jjtAccept(this, data);
                }
            }
        }
        return null;
    }

    @Override
    public Object visit(ASTScript node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTScriptCall node, Object data) {
//        node.childrenAccept(this, data);
        // TODO
        ArrayList<String> scriptCalls = new ArrayList<>();
        String scriptCallIdentifier = ((ASTIdentifier) node.jjtGetChild(0)).getValue();
        scriptCalls.add(scriptCallIdentifier);
        if (node.jjtGetNumChildren() > 1)
            scriptCalls.addAll((Collection<String>) node.jjtGetChild(1).jjtAccept(this, data));
        return scriptCalls;
    }

    @Override
    public Object visit(ASTStmt node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTIfStmt node, Object data) {
//        node.childrenAccept(this, data);
        // TODO
        if (node.jjtGetNumChildren() == 2) {
            BoolLabel b = new BoolLabel(FALL, data.toString());
            node.jjtGetChild(0).jjtAccept(this, b);
            node.jjtGetChild(1).jjtAccept(this, data);
        } else {
            BoolLabel b = new BoolLabel(FALL, newLabel());
            node.jjtGetChild(0).jjtAccept(this, b);
            node.jjtGetChild(1).jjtAccept(this, data);
            m_writer.print("goto " + data.toString() + "\n");
            m_writer.print(b.lFalse + "\n");
            node.jjtGetChild(2).jjtAccept(this, data);
        }
        return null;
    }

    @Override
    public Object visit(ASTWhileStmt node, Object data) {
//        node.childrenAccept(this, data);
        // TODO
        String begin = newLabel();
        m_writer.print(begin + "\n");
        BoolLabel b = new BoolLabel(FALL, data.toString());
        node.jjtGetChild(0).jjtAccept(this, b);
        node.jjtGetChild(1).jjtAccept(this, begin);
        m_writer.print("goto " + begin + "\n");
        return null;
    }

    @Override
    public Object visit(ASTForStmt node, Object data) {
//        node.childrenAccept(this, data);
        // TODO
        node.jjtGetChild(0).jjtAccept(this, data.toString());
        String begin = newLabel();
        String end = newLabel();
        m_writer.print(begin + "\n");
        BoolLabel b = new BoolLabel(newLabel(), data.toString());
        node.jjtGetChild(1).jjtAccept(this, b);
        m_writer.print(b.lTrue + "\n");
        node.jjtGetChild(3).jjtAccept(this, end);
        m_writer.print(end + "\n");
        node.jjtGetChild(2).jjtAccept(this, data);
        m_writer.print("goto " + begin + "\n");
        return null;
    }

    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        String identifier = ((ASTIdentifier) node.jjtGetChild(0)).getValue();
//        node.jjtGetChild(1).jjtAccept(this, data);
        // TODO
        if (SymbolTable.get(identifier) == IntermediateCodeGenVisitor.VarType.Number) {
            String addr = node.jjtGetChild(1).jjtAccept(this, data).toString();
            m_writer.println(identifier + " = " + addr);
        } else if (SymbolTable.get(identifier) == IntermediateCodeGenVisitor.VarType.Bool) {
            BoolLabel b = new BoolLabel(FALL, newLabel());
            node.jjtGetChild(1).jjtAccept(this, b);
            m_writer.print(identifier + " = 1" + "\n");
            m_writer.print("goto " + data.toString() + "\n");
            m_writer.print(b.lFalse + "\n");
            m_writer.print(identifier + " = 0" + "\n");
        }
        return id;
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

        if (node.jjtGetNumChildren() == 1) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        }

        String relop = node.getOps().get(0).toString();

        switch (relop) {
            case "&&":
                BoolLabel b1 = new BoolLabel(FALL, ((BoolLabel) data).lFalse);
                if (((BoolLabel) data).lFalse.equals(FALL))
                    b1.lFalse = newLabel();

                BoolLabel b2 = new BoolLabel(((BoolLabel) data).lTrue, ((BoolLabel) data).lFalse);
                if (((BoolLabel) data).lFalse.equals(FALL)) {
                    node.jjtGetChild(0).jjtAccept(this, b1);
                    node.jjtGetChild(1).jjtAccept(this, b2);
                    m_writer.print(b1.lFalse + "\n");
                } else {
                    node.jjtGetChild(0).jjtAccept(this, b1);
                    node.jjtGetChild(1).jjtAccept(this, b2);
                }
                break;
            case "||":
                BoolLabel b3 = new BoolLabel(((BoolLabel) data).lTrue, FALL);
                if (((BoolLabel) data).lTrue.equals(FALL))
                    b3.lTrue = newLabel();

                BoolLabel b4 = new BoolLabel(((BoolLabel) data).lTrue, ((BoolLabel) data).lFalse);
                if (((BoolLabel) data).lTrue.equals(FALL)) {
                    node.jjtGetChild(0).jjtAccept(this, b3);
                    node.jjtGetChild(1).jjtAccept(this, b4);
                    m_writer.print(b3.lTrue + "\n");
                } else {
                    node.jjtGetChild(0).jjtAccept(this, b3);
                    node.jjtGetChild(1).jjtAccept(this, b4);
                }
                break;
            default:
                return null;
        }
        return null;
    }

    @Override
    public Object visit(ASTCompExpr node, Object data) {
//        node.childrenAccept(this, data);
        // TODO
        if (node.jjtGetNumChildren() == 1) {
            return node.jjtGetChild(0).jjtAccept(this, data);
        }
        if (!((BoolLabel) data).lTrue.equals(FALL) && !((BoolLabel) data).lFalse.equals(FALL)) {
            String id1 = node.jjtGetChild(0).jjtAccept(this, data).toString();
            String id2 = node.jjtGetChild(1).jjtAccept(this, data).toString();

            m_writer.println("if " + id1 + " " + node.getValue() + " " + id2 + " goto " + ((BoolLabel) data).lTrue + "\n");
            m_writer.println("goto " + ((BoolLabel) data).lFalse + "\n");
        } else if (!((BoolLabel) data).lTrue.equals(FALL) && ((BoolLabel) data).lFalse.equals(FALL)) {
            String id1 = node.jjtGetChild(0).jjtAccept(this, data).toString();
            String id2 = node.jjtGetChild(1).jjtAccept(this, data).toString();

            m_writer.println("if " + id1 + " " + node.getValue() + " " + id2 + " goto " + ((BoolLabel) data).lTrue + "\n");
        } else if (((BoolLabel) data).lTrue.equals(FALL) && !((BoolLabel) data).lFalse.equals(FALL)) {
            String id1 = node.jjtGetChild(0).jjtAccept(this, data).toString();
            String id2 = node.jjtGetChild(1).jjtAccept(this, data).toString();

            m_writer.print("ifFalse " + id1 + ' ' + node.getValue() + ' ' + id2 + " goto " + ((BoolLabel) data).lFalse + "\n");
        } else {
            // throw new Exception();
        }

        return node.getValue();

//        else {
//            String id1 = node.jjtGetChild(0).jjtAccept(this, data).toString();
//            String id2 = node.jjtGetChild(1).jjtAccept(this, data).toString();
//
//            m_writer.println("if " + id1 + " " + node.getValue() + " " + id2 + " goto " + ((BoolLabel) data).lTrue);
//            m_writer.println("goto " + ((BoolLabel) data).lFalse);
//            return null;
//        }
    }

    @Override
    public Object visit(ASTNotExpr node, Object data) {
//        node.jjtGetChild(0).jjtAccept(this, data);
        // TODO
        if (node.getOps().size() % 2 == 0)
            return node.jjtGetChild(0).jjtAccept(this, data);

        BoolLabel B1 = new BoolLabel(((BoolLabel) data).lFalse, ((BoolLabel) data).lTrue);
        return node.jjtGetChild(0).jjtAccept(this, B1);
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
        if (node.getValue()) {
            if (!((BoolLabel) data).lTrue.equals(FALL))
                m_writer.print("goto " + ((BoolLabel) data).lTrue + "\n");
        } else {
            if (!((BoolLabel) data).lFalse.equals(FALL))
                m_writer.print("goto " + ((BoolLabel) data).lFalse + "\n");
        }
//        m_writer.print("goto " + (node.getValue() ? ((BoolLabel) data).lTrue : ((BoolLabel) data).lFalse) + "\n");
        return node.getValue();
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) {
        // TODO
        if (SymbolTable.get(node.getValue()) == IntermediateCodeGenVisitor.VarType.Bool) {
            if (!((BoolLabel) data).lTrue.equals(FALL) && !((BoolLabel) data).lFalse.equals(FALL)) {
                m_writer.print("if " + node.getValue() + " == 1 goto " + ((BoolLabel) data).lTrue + "\n");
                m_writer.print("goto " + ((BoolLabel) data).lFalse + "\n");
            } else if (!((BoolLabel) data).lTrue.equals(FALL) && ((BoolLabel) data).lFalse.equals(FALL)) {
                m_writer.print("if " + node.getValue() + " == 1 goto " + ((BoolLabel) data).lTrue + "\n");
            } else if (((BoolLabel) data).lTrue.equals(FALL) && !((BoolLabel) data).lFalse.equals(FALL)) {
                m_writer.print("ifFalse " + node.getValue() + " == 1 goto " + ((BoolLabel) data).lFalse + "\n");
            } else {
                // throw new Exception();
            }
        }

//        if (SymbolTable.get(node.getValue()) == IntermediateCodeGenVisitor.VarType.Bool) {
//            m_writer.println("if " + node.getValue() + " == 1 goto " + ((BoolLabel) data).lTrue);
//            m_writer.println("goto " + ((BoolLabel) data).lFalse);
//        }
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
