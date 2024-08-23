package vanstudio.sequence.agent;

public class CreateNewPseudocodeTaskAction extends CreateNewDesignationTaskAction {

    /**
     * only accept "", designation or pseudocode.
     * @return 从生成设计文档开始
     */
    protected String getRequirementDevProcess() {
        return "pseudocode";
    }

}
