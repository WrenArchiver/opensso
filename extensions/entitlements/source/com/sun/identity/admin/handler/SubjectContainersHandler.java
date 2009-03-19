package com.sun.identity.admin.handler;

import com.icesoft.faces.context.effects.Effect;
import com.icesoft.faces.context.effects.Fade;
import com.icesoft.faces.context.effects.SlideDown;
import com.icesoft.faces.context.effects.SlideUp;
import com.sun.identity.admin.model.MultiPanelBean;
import com.sun.identity.admin.model.SubjectContainer;
import java.io.Serializable;
import java.util.List;
import javax.faces.event.ActionEvent;

public class SubjectContainersHandler implements MultiPanelHandler, Serializable {

    private List<SubjectContainer> subjectContainers;

    public void expandListener(ActionEvent event) {
        MultiPanelBean mpb = (SubjectContainer) event.getComponent().getAttributes().get("bean");
        assert (mpb != null);

        Effect e;
        if (mpb.isExpanded()) {
            e = new SlideUp();
        } else {
            e = new SlideDown();
        }

        e.setTransitory(false);
        e.setSubmit(true);
        mpb.setExpandEffect(e);
    }

    public void removeListener(ActionEvent event) {
        SubjectContainer subjectContainer = (SubjectContainer) event.getComponent().getAttributes().get("bean");
        assert (subjectContainer != null);

        Effect e = new Fade();
        e.setSubmit(true);
        e.setTransitory(false);
        subjectContainer.setPanelEffect(e);
    }

    public List<SubjectContainer> getSubjectContainers() {
        return subjectContainers;
    }

    public void setSubjectContainers(List<SubjectContainer> subjectContainers) {
        this.subjectContainers = subjectContainers;
    }
}
