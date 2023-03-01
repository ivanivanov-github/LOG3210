Après avoir eu l'accord d'un des chargé, nous avons changé l'attribut data qui est passé comme paramètre au
node.childrenAccept dans la fonction visit du ASTProgram par new DataStruct(). Nous avons décidé de faire cela
car nous avions besoin d'avoir le type du data dans la visit d'un ASTBlock. Hors si nous mettions l'instantiation
du DataStruct dans Block, on aurait eu un overwrite de la valeur type et on ne pourrait pas faire notre vérification.