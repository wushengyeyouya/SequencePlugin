package vanstudio.sequence.ext.uast;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import com.intellij.util.Query;
import com.intellij.util.containers.Stack;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.*;
import org.jetbrains.uast.visitor.AbstractUastVisitor;
import vanstudio.sequence.config.SequenceSettingsState;
import vanstudio.sequence.generator.filters.ImplementClassFilter;
import vanstudio.sequence.openapi.IGenerator;
import vanstudio.sequence.openapi.SequenceParams;
import vanstudio.sequence.openapi.model.CallStack;
import vanstudio.sequence.openapi.model.MethodDescription;
import vanstudio.sequence.util.MyPsiUtil;
import vanstudio.sequence.util.MyUastUtilKt;

import java.util.*;
import java.util.stream.Collectors;

public class UastSequenceGenerator extends AbstractUastVisitor implements IGenerator {
    private static final Logger LOGGER = Logger.getInstance(UastSequenceGenerator.class);

    private final Stack<Integer> offsetStack = new Stack<>();

    private final ArrayList<String> imfCache = new ArrayList<>();
    private CallStack topStack;
    private CallStack currentStack;
    private final SequenceParams params;

    private final boolean SHOW_LAMBDA_CALL;
    private final boolean SMART_INTERFACE;

    public UastSequenceGenerator(SequenceParams params) {
        this.params = params;
        SHOW_LAMBDA_CALL = SequenceSettingsState.getInstance().SHOW_LAMBDA_CALL;
        SMART_INTERFACE = SequenceSettingsState.getInstance().SMART_INTERFACE;
    }

    public UastSequenceGenerator(SequenceParams params, int offset) {
        this(params);
        offsetStack.push(offset);
    }

    @Override
    public CallStack generate(PsiElement psiElement, @Nullable CallStack parent) {
        if (parent != null) {
            topStack = parent;
            currentStack = topStack;
        }
        if (psiElement instanceof UMethod) {
            generateMethod((UMethod) psiElement);
        } else {
            UMethod uMethod = UastContextKt.toUElement(psiElement, UMethod.class);
            generateMethod(uMethod);
        }
        return topStack;
    }

    private void generateLambda(ULambdaExpression node) {
        MethodDescription method = MyUastUtilKt.createMethod(node, MyPsiUtil.findNaviOffset(node.getSourcePsi()));
        if(method == null) {
            return;
        }
        makeMethodCallExceptCurrentStackIsRecursive(method);
        node.getBody().accept(this);
    }

    private void generateMethod(UMethod uMethod) {
        if (uMethod == null) {
            return;
        }
        UClass containingUClass = UastUtils.getContainingUClass(uMethod);

        if (containingUClass != null && containingUClass.isInterface() && !MyUastUtilKt.isExternal(containingUClass)) {
            uMethod.accept(this);

            // follow implementation
            PsiElement sourcePsi = uMethod.getSourcePsi();
            if (sourcePsi != null) {
                Query<PsiElement> search = DefinitionsScopedSearch.search(sourcePsi).allowParallelProcessing();
                Collection<PsiElement> searched = search.findAll();
                if (searched.isEmpty()) {
                    return;
                }
                PsiElement selectedPsiMethod;
                if (searched.size() > 1) {
                    Map<String, PsiElement> className2Method = searched.stream().map(psiElement -> {
                        UMethod method = UastContextKt.toUElement(psiElement, UMethod.class);
                        if (method != null && method.getContainingClass() != null) {
                            return new Object[]{method.getContainingClass().getQualifiedName(), psiElement};
                        } else {
                            return null;
                        }
                    }).filter(Objects::nonNull).collect(Collectors.toMap(array -> (String) array[0], array -> (PsiElement) array[1]));
                    if (className2Method.isEmpty()) {
                        return;
                    }
                    String[] classes = className2Method.keySet().toArray(new String[0]);
                    String selectedClass = Messages.showEditableChooseDialog(uMethod.getContainingClass().getQualifiedName(), "Ensure implemetation(确认继承关系)",
                            Messages.getQuestionIcon(), classes, classes[0], null);
                    selectedPsiMethod = className2Method.get(selectedClass);
                } else {
                    selectedPsiMethod = (PsiMethod) CollectionUtils.get(searched, 0);
                }
                UMethod selectedMethod = UastContextKt.toUElement(selectedPsiMethod, UMethod.class);
                if (selectedMethod != null && params.getMethodFilter().allow(selectedPsiMethod)) {
                    selectedMethod.accept(this);
                }
            }
        } else {
            // resolve variable initializer
            if (SMART_INTERFACE
                    && !MyUastUtilKt.isExternal(containingUClass)
                    && containingUClass != null
                    && !imfCache.contains(containingUClass.getQualifiedName())) {
                containingUClass.accept(new MyImplFinder());
                imfCache.add(containingUClass.getQualifiedName());
            }
            uMethod.accept(this);
        }
    }

    private boolean makeMethodCallExceptCurrentStackIsRecursive(MethodDescription method) {
        if (topStack == null) {
            topStack = new CallStack(method);
            currentStack = topStack;
        } else {
            if (params.isNotAllowRecursion() && currentStack.isRecursive(method))
                return true;
            currentStack = currentStack.methodCall(method);
        }
        return false;
    }

    private void methodCall(PsiMethod psiMethod, int offset) {
        if (psiMethod == null) return;
        if (!params.getMethodFilter().allow(psiMethod)) return;

        if (currentStack.level() < params.getMaxDepth()) {
            CallStack oldStack = currentStack;
            LOGGER.debug("+ depth = " + currentStack.level() + " method = " + psiMethod.getName());
            offsetStack.push(offset);
            UMethod uMethod = UastContextKt.toUElement(psiMethod, UMethod.class);
            generateMethod(uMethod);
            LOGGER.debug("- depth = " + currentStack.level() + " method = " + psiMethod.getName());
            currentStack = oldStack;
        } else {
            UMethod uMethod = UastContextKt.toUElement(psiMethod, UMethod.class);
            if (uMethod != null) {
                MethodDescription methodDescription = MyUastUtilKt.createMethod(uMethod, offset);
                if(methodDescription != null) {
                    currentStack.methodCall(methodDescription);
                }
            }
        }
    }

    // -- visitor -- //
    @Override
    public boolean visitMethod(@NotNull UMethod node) {
        int offset = offsetStack.isEmpty() ? MyPsiUtil.findNaviOffset(node.getSourcePsi()) : offsetStack.pop();

        MethodDescription method = MyUastUtilKt.createMethod(node, offset);
        if (method == null) {
            return false;
        }
        return makeMethodCallExceptCurrentStackIsRecursive(method);
//        return super.visitMethod(node);
    }

    @Override
    public boolean visitCallExpression(@NotNull UCallExpression node) {
        boolean isComplexCall = false;
        List<UExpression> valueArguments = node.getValueArguments();
        for (UExpression valueArgument : valueArguments) {
            if (valueArgument instanceof UQualifiedReferenceExpression
                    /* || valueArgument instanceof ULambdaExpression*/
                    || valueArgument instanceof UCallExpression
                /* || valueArgument instanceof UCallableReferenceExpression*/) {
                // generate value argument before call expression
                valueArgument.accept(this);
                isComplexCall = true;
            }
        }
        PsiMethod method = node.resolve();
        methodCall(method, MyPsiUtil.findNaviOffset(node.getSourcePsi()));
        return isComplexCall;
    }


    @Override
    public boolean visitLambdaExpression(@NotNull ULambdaExpression node) {
        if (SHOW_LAMBDA_CALL) {
            // generate dummy () -> call, and it's body in separate generator
            generateLambda(node);
            //true:  do not need to generate lambda body in this generator.
            return true;
        }
        return super.visitLambdaExpression(node);
    }

    @Override
    public boolean visitCallableReferenceExpression(@NotNull UCallableReferenceExpression node) {
        final PsiElement resolve = node.resolve();
        if (resolve instanceof PsiMethod) {
            final PsiMethod psiMethod = (PsiMethod) resolve;
            final int offset = MyPsiUtil.findNaviOffset(node.getSourcePsi());
            methodCall(psiMethod, offset);
        }
        return super.visitCallableReferenceExpression(node);
    }

    @Override
    public boolean visitDeclaration(@NotNull UDeclaration node) {
        if (SMART_INTERFACE && node instanceof ULocalVariable) {
            ULocalVariable localVariable = (ULocalVariable) node;
            variableImplementationFinder(localVariable.getTypeReference(), localVariable.getUastInitializer());
        }
        return super.visitDeclaration(node);
    }

    @Override
    public boolean visitBinaryExpression(@NotNull UBinaryExpression node) {
        UExpression uExpression = node.getRightOperand();
        if (SMART_INTERFACE && uExpression instanceof UCallExpression) {
            findAssignmentImplFilter(node.getLeftOperand().getExpressionType(), uExpression);
        }
        return super.visitBinaryExpression(node);
    }

    private void variableImplementationFinder(UTypeReferenceExpression typeReference, UExpression uastInitializer) {
        if (typeReference == null || uastInitializer == null) return;

        String face = typeReference.getQualifiedName();

        if (face == null) return;

        if (uastInitializer instanceof UCallExpression) {
            UastCallKind kind = ((UCallExpression) uastInitializer).getKind();
            if (kind.equals(UastCallKind.CONSTRUCTOR_CALL)) {
                PsiType initializerType = uastInitializer.getExpressionType();
                if (initializerType != null) {
                    ArrayList<String> list = new ArrayList<>();

                    String impl = initializerType.getCanonicalText();
                    if (!face.equals(impl)) {
                        list.add(impl);
                    }

                    PsiType[] superTypes = initializerType.getSuperTypes();
                    for (PsiType superType : superTypes) {
                        String superImpl = superType.getCanonicalText();
                        if (!face.equals(superImpl)) {
                            list.add(superImpl);
                        }
                    }

                    if (!list.isEmpty()) {
                        params.getImplementationWhiteList().putIfAbsent(face, new ImplementClassFilter(list.toArray(new String[0])));
                    }
                }
            }
        }


    }

    private void findAssignmentImplFilter(PsiType psiType, UExpression expression) {

        if (expression instanceof UCallExpression) {
            UastCallKind kind = ((UCallExpression) expression).getKind();
            if (kind.equals(UastCallKind.CONSTRUCTOR_CALL)) {
                String face = psiType.getCanonicalText();
                PsiType type = expression.getExpressionType();
                if (type != null) {
                    String impl = type.getCanonicalText();
                    if (!impl.equals(face)) {
                        params.getImplementationWhiteList().putIfAbsent(face, new ImplementClassFilter(impl));
                    }
                }
            }

        }
    }


    /**
     * Find interface -> implementation in assignment
     */
    private class MyImplFinder extends AbstractUastVisitor {
//        @Override
//        public boolean visitClass(@NotNull UClass node) {
//            List<UTypeReferenceExpression> uastSuperTypes = node.getUastSuperTypes();
//            for (UTypeReferenceExpression uastSuperType : uastSuperTypes) {
////                if (!MyUastUtilKt.isExternal(uastSuperType)) {
//                uastSuperType.accept(this);
////                }
//            }
//
//            return node.isInterface() || MyUastUtilKt.isExternal(node);
//        }

        @Override
        public boolean visitField(@NotNull UField node) {
            UTypeReferenceExpression typeReference = node.getTypeReference();
            UExpression uastInitializer = node.getUastInitializer();
            variableImplementationFinder(typeReference, uastInitializer);
            return super.visitField(node);
        }
//
//        @Override
//        public boolean visitMethod(@NotNull UMethod node) {
//            return !node.isConstructor();
//        }

    }
}
