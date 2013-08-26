
JAVA_FILES =  AST.java ASTNode.java ASTNodeIterator.java Argument.java \
              ArrayCreationExpression.java ArrayInitializer.java \
              ArrayType.java AssertStatement.java Assignment.java \
              AtLeastQuantifierExpression.java \
              AtMostQuantifierExpression.java \
              BinaryConstraintExpression.java BinaryExpression.java \
              Block.java BreakStatement.java CastExpression.java \
              CatchClause.java CatchList.java ClassifierAssignment.java \
              ClassifierCastExpression.java ClassifierExpression.java \
              ClassifierExpressionList.java ClassifierName.java \
              ClassifierReturnType.java ClassifierType.java \
              CodedClassifier.java CompositeGenerator.java Conditional.java \
              Conjunction.java Constant.java ConstantList.java \
              ConstraintDeclaration.java ConstraintEqualityExpression.java \
              ConstraintExpression.java ConstraintInvocation.java \
              ConstraintStatementExpression.java ConstraintType.java \
              ContinueStatement.java Declaration.java DeclarationList.java \
              DoStatement.java EmptyStatement.java \
              ExistentialQuantifierExpression.java Expression.java \
              ExpressionList.java ExpressionStatement.java FieldAccess.java \
              ForStatement.java IfStatement.java ImportDeclaration.java \
              ImportList.java IncrementExpression.java \
              InferenceDeclaration.java InferenceInvocation.java \
              InferenceType.java InstanceCreationExpression.java \
              InstanceofExpression.java LabeledStatement.java \
              LearningClassifierExpression.java List.java \
              MethodInvocation.java Name.java NameList.java \
              NegatedConstraintExpression.java NormalizerType.java \
              Operator.java PackageDeclaration.java \
              ParameterSet.java PrimitiveType.java \
              QuantifiedConstraintExpression.java ReferenceType.java \
              ReturnStatement.java SenseStatement.java Statement.java \
              StatementExpression.java StatementList.java \
              SubscriptVariable.java SwitchBlock.java SwitchGroup.java \
              SwitchGroupList.java SwitchLabel.java SwitchLabelList.java \
              SwitchStatement.java SymbolTable.java \
              SynchronizedStatement.java ThrowStatement.java \
              TryStatement.java Type.java UnaryExpression.java \
              UniversalQuantifierExpression.java VariableDeclaration.java \
              VariableInstance.java WhileStatement.java

if USING_JAVAC
AM_JAVACFLAGS = -source 1.4 @JAVA_DEBUG@
endif

include $(top_srcdir)/JavaMake.am

EXTRA_DIST =  $(JAVA_FILES) package.html

