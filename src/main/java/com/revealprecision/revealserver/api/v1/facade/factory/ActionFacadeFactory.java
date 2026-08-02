package com.revealprecision.revealserver.api.v1.facade.factory;

import com.revealprecision.revealserver.api.v1.dto.request.EffectivePeriod;
import com.revealprecision.revealserver.api.v1.facade.models.FormFacade;
import com.revealprecision.revealserver.api.v1.dto.request.SubjectCodableConcept;
import com.revealprecision.revealserver.api.v1.facade.models.ActionFacade;
import com.revealprecision.revealserver.persistence.domain.Action;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ActionFacadeFactory {

  public static ActionFacade fromEntity(Action action) {
    ActionFacade build = ActionFacade.builder()
        .identifier(action.getIdentifier().toString())
        .title(action.getTitle())
        .description(action.getDescription())
        .code(action.getTitle()) //addCode
        .timingPeriod(EffectivePeriod.builder()
            .start(action.getTimingPeriodStart())
            .end(action.getTimingPeriodEnd()).build())
        .reason("") //addReason
        .goalId(action.getGoal().getIdentifier().toString())
        .subjectCodableConcept(
            SubjectCodableConcept.builder().text("").build())//addSubject
        .type(action.getType())
        .build();

    if (action.getForm() != null ){
      FormFacade build1 = FormFacade.builder()
          .name(action.getForm().getName())
          .build();
      if (action.getForm().isTemplate()){
        build1.setTemplate(action.getForm().getTitle());
      }
      build.setForm(
          build1);
    }

    return build;

  }
}
