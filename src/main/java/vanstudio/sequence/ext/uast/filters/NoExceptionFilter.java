package vanstudio.sequence.ext.uast.filters;

import com.intellij.psi.PsiElement;
import org.jetbrains.uast.UMethod;
import org.jetbrains.uast.UastContextKt;
import vanstudio.sequence.openapi.filters.MethodFilter;

public class NoExceptionFilter implements MethodFilter {

    @Override
    public boolean allow(PsiElement psiElement) {
        UMethod uMethod = UastContextKt.toUElement(psiElement, UMethod.class);
        if (uMethod == null) {
            return false;
        } else if (uMethod.getContainingClass() == null || uMethod.getContainingClass().getName() == null) {
            return true;
        }
        return !uMethod.getContainingClass().getName().endsWith("Exception");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return !(o == null || getClass() != o.getClass());
    }

}
