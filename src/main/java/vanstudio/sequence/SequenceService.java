package vanstudio.sequence;

import com.intellij.psi.PsiElement;

/**
 * &copy; fanhuagang@gmail.com
 * Created by van on 2020/2/23.
 */
public interface SequenceService {
    String PLUGIN_ID = "SequenceDiagram";
    String PLUGIN_NAME = "BDP-Agent";

    void showSequence(PsiElement psiElement);

}
