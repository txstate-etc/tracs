/**********************************************************************************
 * 
 * 
 ***********************************************************************************
 *
 * Copyright (c) 2006, 2008 The Sakai Foundation
 *
 * Licensed under the Educational Community License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.osedu.org/licenses/ECL-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **********************************************************************************/



package org.sakaiproject.tool.assessment.ui.listener.evaluation;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.sakaiproject.tool.assessment.data.dao.grading.AssessmentGradingData;
import org.sakaiproject.tool.assessment.facade.AgentFacade;
import org.sakaiproject.tool.assessment.facade.PublishedAssessmentFacade;
import org.sakaiproject.tool.assessment.data.dao.assessment.PublishedAssessmentData;
import org.sakaiproject.tool.assessment.services.GradebookServiceException;
import org.sakaiproject.tool.assessment.services.GradingService;
import org.sakaiproject.tool.assessment.ui.bean.evaluation.TotalScoresBean;
import org.sakaiproject.tool.assessment.ui.listener.util.ContextUtil;
import org.sakaiproject.tool.assessment.ui.bean.evaluation.AgentResults;
import org.sakaiproject.tool.assessment.services.assessment.PublishedAssessmentService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * <p>Title: Samigo</p>
 * <p>Purpose:  this module set total scores of those unsubmitted students
 * <p> to be 0's requested in bugid:3213 implemented locally in TRACS.
 * <p>Description: Sakai Assessment Manager</p>
 * @author Yuanhua Qu at Texas State University
 * Date: 12/6/2010
 * @version 
 */

public class SetUnsubmittedTotalScoreListener implements ActionListener
{
  private static Log log = LogFactory.getLog(SetUnsubmittedTotalScoreListener.class);
  private static ContextUtil cu;
  
  /**
   * ACTION.
   * @param ae
   * @throws AbortProcessingException
   */
  public void processAction(ActionEvent ae) throws
    AbortProcessingException
  {
    TotalScoresBean bean = (TotalScoresBean) cu.lookupBean("totalScores");
    ArrayList students_not_submitted= new ArrayList();
    prepareNotSubmittedAgent(bean,students_not_submitted);
    if (!saveNotSubmittedAgentScore(students_not_submitted.iterator(), bean))
    	throw new RuntimeException("failed to call saveNotSubmittedAgentScore.");
    bean.setAssessmentGradingList(new ArrayList());
    //setAssignZeros -Qu for bugid:5399
    setAssignZerosForPublishedAssessment(bean);
  }
  
  //Set grade in gradebook to be 0 for not submitted students
  public boolean saveNotSubmittedAgentScore(Iterator notsubmitted_iter,
    TotalScoresBean bean) {
    log.debug("TotalScoreListener: saveNotSubmittedAgentResult starts");
    ArrayList grading = new ArrayList();
    while (notsubmitted_iter.hasNext()) {
         String studentid = (String) notsubmitted_iter.next();
         AgentResults results = new AgentResults();
         AgentFacade agent = new AgentFacade(studentid);
         AssessmentGradingData data = new AssessmentGradingData();
         data.setAgentId(agent.getIdString());
         data.setForGrade(Boolean.TRUE);
         data.setStatus(new Integer(1));
         data.setIsLate(Boolean.FALSE);
         data.setItemGradingSet(new HashSet());
         data.setPublishedAssessmentId(bean.getPublishedAssessment().getPublishedAssessmentId());
         data.setAssessmentGradingId(new Long(0));
         data.setSubmittedDate(null);
         data.setTotalAutoScore(new Double(0));
         data.setTotalOverrideScore(new Double(0));
         data.setFinalScore(new Double(0));
         data.setComments(null);
         grading.add(data);
    }

    try{
         GradingService delegate = new GradingService();
         for(int i=0; i<grading.size();i++){
              delegate.notifyGradebook((AssessmentGradingData)grading.get(i), bean.getPublishedAssessment());
         }
         String success=(String)ContextUtil.getLocalizedString("org.sakaiproject.tool.assessment.bundle.EvaluationMessages", "setToZero_success");
         FacesContext context = FacesContext.getCurrentInstance();
         context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO,success,null));
         log.debug("Saved scores in gradebook.");
    } catch (GradebookServiceException ge) {
         String err=(String)ContextUtil.getLocalizedString("org.sakaiproject.tool.assessment.bundle.AuthorMessages", "gradebook_exception_error");
         FacesContext context = FacesContext.getCurrentInstance();
         context.addMessage(null, new FacesMessage(err));
         return true;
    }
    return true;
  }
  
  public void prepareNotSubmittedAgent(TotalScoresBean bean, ArrayList students_not_submitted){
      ArrayList allscores = bean.getAssessmentGradingList();
      Map useridMap= bean.getUserIdMap(TotalScoresBean.CALLED_FROM_TOTAL_SCORE_LISTENER);
      Iterator allscores_iter = allscores.iterator();
      ArrayList students_submitted= new ArrayList();
      while (allscores_iter.hasNext())
      {
        AssessmentGradingData data = (AssessmentGradingData) allscores_iter.next();
        String agentid =  data.getAgentId();
        
        // get the Map of all users(keyed on userid) belong to the selected sections 
        // now we only include scores of users belong to the selected sections
        if (useridMap.containsKey(agentid)) {
          students_submitted.add(agentid);
        }
      }

      // now get the list of students that have not submitted for grades 
      Iterator useridIterator = useridMap.keySet().iterator(); 
      while (useridIterator.hasNext()) {
        String userid = (String) useridIterator.next(); 	
        if (!students_submitted.contains(userid)) {
          students_not_submitted.add(userid);
        }
      }
  }
  
  public void setAssignZerosForPublishedAssessment(TotalScoresBean bean)  {
	    PublishedAssessmentService publishedAssesService = new PublishedAssessmentService();
	    PublishedAssessmentFacade pubAssessment = publishedAssesService.getPublishedAssessment(bean.getPublishedAssessment().getPublishedAssessmentId().toString());
	    ((PublishedAssessmentData)pubAssessment.getData()).setAssignZeros(true);
	    publishedAssesService.saveAssessment(pubAssessment);
  }
}
