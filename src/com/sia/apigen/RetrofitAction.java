package com.sia.apigen;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.lang.dart.psi.DartClass;
import com.jetbrains.lang.dart.psi.DartClassDefinition;
import com.jetbrains.lang.dart.psi.DartComponent;
import com.jetbrains.lang.dart.psi.DartMethodDeclaration;
import org.jetbrains.annotations.NotNull;


public class RetrofitAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        // TODO: insert action logic here
        Project project = e.getData(CommonDataKeys.PROJECT);
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile psiFile = e.getData(CommonDataKeys.PSI_FILE);

        if (project != null && editor != null && psiFile != null) {
            int caretOffset = editor.getCaretModel().getOffset();
            //指针位置的方法
            DartMethodDeclaration method = PsiTreeUtil.getParentOfType(psiFile.findElementAt(caretOffset), DartMethodDeclaration.class);
            //指针位置的类
            DartClass dartClass = PsiTreeUtil.getParentOfType(psiFile.findElementAt(caretOffset), DartClassDefinition.class);

            if (method != null) {
                new RetrofitActionFix(project, editor, method, dartClass, getDesStartRange(dartClass, psiFile)).process();
            }
        }
    }

    /**
     * 找到生成的代码存放位置
     *
     * @param initClass 抽象接口类
     * @param psiFile   文件
     * @return 文件中在抽象接口类下方的第一个类的 EndOffset，如果在文件中没有找到，那么默认在方法下方
     */
    private int getDesStartRange(DartClass initClass, PsiFile psiFile) {
        if (initClass == null) {
            return 0;
        }

        for (int i = 0; i < 100; i++) {
            DartClass targetClass = PsiTreeUtil.getParentOfType(psiFile.findElementAt(initClass.getTextRange().getEndOffset() + i), DartClassDefinition.class);
            if (targetClass == null) {
                continue;
            }
            return targetClass.getTextRange().getEndOffset() - 1;
        }
        return 0;
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);
        Pair<Editor, PsiFile> pair = getEditorAndPsiFile(e);
        Editor editor = pair.first;
        PsiFile psiFile = pair.second;
        int caretOffset = -1;
        if (editor != null) {
            caretOffset = editor.getCaretModel().getOffset();
        }
        boolean enable = psiFile != null &&
                doEnable(PsiTreeUtil.getParentOfType(psiFile.findElementAt(caretOffset), DartComponent.class));

        e.getPresentation().setEnabledAndVisible(enable);
    }

    private boolean doEnable(DartComponent component) {
        if (component != null && component instanceof DartMethodDeclaration) {
            return true;
        } else {
            return false;
        }
    }


    private Pair<Editor, PsiFile> getEditorAndPsiFile(AnActionEvent e) {
        if (e.getData(CommonDataKeys.PROJECT) == null) {
            return new Pair<>(null, null);
        } else {
            return new Pair<>(e.getData(CommonDataKeys.EDITOR), e.getData(CommonDataKeys.PSI_FILE));
        }
    }
}
