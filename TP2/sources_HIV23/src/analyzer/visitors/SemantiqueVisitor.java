package analyzer.visitors;

import analyzer.SemantiqueError;
import analyzer.ast.*;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created: 19-01-10
 * Last Changed: 03-02-23
 * Author: Félix Brunet
 * <p>
 * Description: Ce visiteur explorer l'AST est renvois des erreurs lorsqu'une erreur sémantique est détectée.
 */

public class SemantiqueVisitor implements ParserVisitor {

    private final PrintWriter m_writer;

    private HashMap<String, VarType> SymbolTable = new HashMap<>(); // mapping variable -> type

    // variable pour les metrics
    public int VAR = 0;
    public int WHILE = 0;
    public int IF = 0;
    public int FUNC = 0;
    public int OP = 0;

    public SemantiqueVisitor(PrintWriter writer) {
        m_writer = writer;
    }

    /*
    IMPORTANT:
    *
    * L'implémentation des visiteurs se base sur la grammaire fournie (Langage.jjt). Il faut donc la consulter pour
    * déterminer les noeuds enfants à visiter. Cela vous sera utile pour lancer les erreurs au bon moment.
    * Pour chaque noeud, on peut :
    *   1. Déterminer le nombre d'enfants d'un noeud : jjtGetNumChildren()
    *   2. Visiter tous les noeuds enfants: childrenAccept()
    *   3. Accéder à un noeud enfant : jjtGetChild()
    *   4. Visiter un noeud enfant : jjtAccept()
    *   5. Accéder à m_value (type) ou m_ops (vecteur des opérateurs) selon la classe de noeud AST (src/analyser/ast)
    *
    * Cela permet d'analyser l'intégralité de l'arbre de syntaxe abstraite (AST) et d'effectuer une analyse sémantique du code.
    *
    * Le Visiteur doit lancer des erreurs lorsqu'une situation arrive.
    *
    * Pour vous aider, voici le code à utiliser pour lancer les erreurs :
    *
    * - Utilisation d'identifiant non défini :
    *   throw new SemantiqueError("Invalid use of undefined Identifier " + node.getValue());
    *
    * - Plusieurs déclarations pour un identifiant. Ex : num a = 1; bool a = true; :
    *   throw new SemantiqueError(String.format("Identifier %s has multiple declarations", varName));
    *
    * - Utilisation d'un type num dans la condition d'un if ou d'un while :
    *   throw new SemantiqueError("Invalid type in condition");
    *
    * - Utilisation de types non valides pour des opérations de comparaison :
    *   throw new SemantiqueError("Invalid type in expression");
    *
    * - Assignation d'une valeur à une variable qui a déjà reçu une valeur d'un autre type. Ex : a = 1; a = true; :
    *   throw new SemantiqueError(String.format("Invalid type in assignation of Identifier %s", varName));
    *
    * - Le type de retour ne correspond pas au type de fonction :
    *   throw new SemantiqueError("Return type does not match function type");
    * */


    @Override
    public Object visit(SimpleNode node, Object data) {
        return data;
    }

    @Override
    public Object visit(ASTProgram node, Object data) {
        node.childrenAccept(this, new DataStruct());
        m_writer.print(String.format("{VAR:%d, WHILE:%d, IF:%d, FUNC:%d, OP:%d}", this.VAR, this.WHILE, this.IF, this.FUNC, this.OP));
        return null;
    }

    // Enregistre les variables avec leur type dans la table symbolique.
    @Override
    public Object visit(ASTDeclaration node, Object data) {
        // TODO
        VAR++;
        String varName = ((ASTIdentifier) node.jjtGetChild(0)).getValue();
        if (SymbolTable.containsKey(varName))
            throw new SemantiqueError(String.format("Identifier %s has multiple declarations", varName));

        VarType varType = VarType.Number;

        if (node.getValue().equals(VarType.Bool.toString().toLowerCase()))
            varType = VarType.Bool;

        SymbolTable.put(varName, varType);
        ((DataStruct)data).type = varType;
        return null;
    }

    @Override
    public Object visit(ASTBlock node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTStmt node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }

    // Méthode qui pourrait être utile pour vérifier le type d'expression dans une condition.
    private void callChildenCond(SimpleNode node) {
        DataStruct d = new DataStruct();
        node.jjtGetChild(0).jjtAccept(this, d);

        // TODO
        if (d.type != VarType.Bool)
            throw new SemantiqueError("Invalid type in condition");

        int numChildren = node.jjtGetNumChildren();
        for (int i = 1; i < numChildren; i++) {
            d = new DataStruct();
            node.jjtGetChild(i).jjtAccept(this, d);
        }
    }

    // les structures conditionnelle doivent vérifier que leur expression de condition est de type booléenne
    // On doit aussi compter les conditions dans les variables IF et WHILE
    @Override
    public Object visit(ASTIfStmt node, Object data) {
        // TODO : Fiez vous aux langages pour determiner les noeuds enfants possible
        IF++;
        callChildenCond(node);
        return null;
    }

    @Override
    public Object visit(ASTWhileStmt node, Object data) {
        // TODO
        WHILE++;
        callChildenCond(node);
        return null;
    }

    @Override
    public Object visit(ASTFunctionStmt node, Object data) {
        // TODO
        return null;
    }

    @Override
    public Object visit(ASTFunctionBlock node, Object data) {
        // TODO
        return null;
    }

    @Override
    public Object visit(ASTReturnStmt node, Object data) {
        // TODO
        return null;
    }

    // On doit vérifier que le type de la variable est compatible avec celui de l'expression.
    @Override
    public Object visit(ASTAssignStmt node, Object data) {
        // TODO
        node.childrenAccept(this, data);
        String varName = ((ASTIdentifier)node.jjtGetChild(0)).getValue();

        VarType varType = SymbolTable.get(varName);
        if (!varType.equals(((DataStruct)data).type)) {
            throw new SemantiqueError(String.format("Invalid type in assignation of Identifier %s", varName));
        }

        return null;
    }

    @Override
    public Object visit(ASTExpr node, Object data) {
        // TODO
        node.childrenAccept(this, data);
        return null;
    }

    @Override
    public Object visit(ASTCompExpr node, Object data) {
        /*
            Attention, ce noeud est plus complexe que les autres :
            - S’il n'a qu'un seul enfant, le noeud a pour type le type de son enfant.
            - S’il a plus d'un enfant, alors il s'agit d'une comparaison. Il a donc pour type "Bool".
            - Il n'est pas acceptable de faire des comparaisons de booléen avec les opérateurs < > <= >=.
            - Les opérateurs == et != peuvent être utilisé pour les nombres et les booléens, mais il faut que le type
            soit le même des deux côtés de l'égalité/l'inégalité.
        */
        // TODO
        ArrayList<VarType> types = new ArrayList<>();
        for(int i = 0; i < node.jjtGetNumChildren(); i++){
            node.jjtGetChild(i).jjtAccept(this,data);
            types.add(((DataStruct)data).type);
        }

        if(types.size() == 1)
            return null;

        boolean sameTypeBothSide = types.get(0).equals(types.get(1));
        boolean opEquals = node.getValue().equals("==");
        boolean opNotEquals = node.getValue().equals("!=");
        boolean opNums = types.get(0).equals(VarType.Number);

        if ((!opEquals&& !opNotEquals  && !opNums) || !sameTypeBothSide)
            throw new SemantiqueError("Invalid type in expression");
        ((DataStruct)data).type = VarType.Bool;
        OP++;

        return null;
    }


    /*
        Opérateur binaire :
        - s’il n'y a qu'un enfant, aucune vérification à faire.
        - Par exemple, un AddExpr peut retourner le type "Bool"
        à condition de n'avoir qu'un seul enfant.
     */
    @Override
    public Object visit(ASTAddExpr node, Object data) {
        // TODO
        int numChildren = node.jjtGetNumChildren();
        if (numChildren == 1) {
            node.childrenAccept(this, data);
            return null;
        }
        OP--;
        for (int i = 0; i < numChildren; i++) {
            OP++;
//            DataStruct d = new DataStruct();
            node.jjtGetChild(i).jjtAccept(this, data);
            if (!((DataStruct)data).type.equals(VarType.Number))
                throw new SemantiqueError("Invalid type in expression");
        }
        return null;
    }

    @Override
    public Object visit(ASTMulExpr node, Object data) {
        // TODO
        int numChildren = node.jjtGetNumChildren();
        if (numChildren == 1) {
            node.childrenAccept(this, data);
            return null;
        }
        OP--;
        for (int i = 0; i < numChildren; i++) {
            OP++;
//            DataStruct d = new DataStruct();
            node.jjtGetChild(i).jjtAccept(this, data);
            if (!((DataStruct)data).type.equals(VarType.Number))
                throw new SemantiqueError("Invalid type in expression");
        }
        return null;
    }

    @Override
    public Object visit(ASTBoolExpr node, Object data) {
        // TODO
        int numChildren = node.jjtGetNumChildren();
        if (numChildren == 1) {
            node.childrenAccept(this, data);
            return null;
        }
        OP--;
        for (int i = 0; i < numChildren; i++) {
            OP++;
            // DataStruct d = new DataStruct();
            node.jjtGetChild(i).jjtAccept(this, data);
            if (!((DataStruct)data).type.equals(VarType.Bool))
                throw new SemantiqueError("Invalid type in expression");
        }
        return null;
    }

    /*
        Opérateur unaire
        Les opérateurs unaires ont toujours un seul enfant. Cependant, ASTNotExpr et ASTUnaExpr ont la fonction
        "getOps()" qui retourne un vecteur contenant l'image (représentation str) de chaque token associé au noeud.
        Il est utile de vérifier la longueur de ce vecteur pour savoir si un opérande est présent.
        - S’il n'y a pas d'opérande, ne rien faire.
        - S’il y a un (ou plus) opérande, il faut vérifier le type.
    */
    @Override
    public Object visit(ASTNotExpr node, Object data) {
        // TODO
        node.childrenAccept(this, data);
        if(node.getOps().size() > 0 && !((DataStruct)data).type.equals(VarType.Bool))
            throw new SemantiqueError("Invalid type in expression");
        OP += node.getOps().size();
        return null;
    }

    @Override
    public Object visit(ASTUnaExpr node, Object data) {
        // TODO
        node.childrenAccept(this, data);
        if(node.getOps().size() > 0 && !((DataStruct)data).type.equals(VarType.Number))
            throw new SemantiqueError("Invalid type in expression");

        OP += node.getOps().size();
        return null;
    }

    /*
        Les noeud ASTIdentifier ayant comme parent "GenValue" doivent vérifier leur type.
        On peut envoyer une information à un enfant avec le 2e paramètre de jjtAccept ou childrenAccept.
     */
    @Override
    public Object visit(ASTGenValue node, Object data) {
        node.childrenAccept(this, data);
        return null;
    }


    @Override
    public Object visit(ASTBoolValue node, Object data) {
        ((DataStruct) data).type = VarType.Bool;
        return null;
    }

    @Override
    public Object visit(ASTIdentifier node, Object data) {

        if (node.jjtGetParent() instanceof ASTGenValue) {
            String varName = node.getValue();
            VarType varType = SymbolTable.get(varName);

            ((DataStruct) data).type = varType;
        }

        return null;
    }

    @Override
    public Object visit(ASTIntValue node, Object data) {
        ((DataStruct) data).type = VarType.Number;
        return null;
    }


    //des outils pour vous simplifier la vie et vous enligner dans le travail
    public enum VarType {
        Bool,
        Number
    }


    private class DataStruct {
        public VarType type;

        public DataStruct() {
        }

        public DataStruct(VarType p_type) {
            type = p_type;
        }

    }
}
