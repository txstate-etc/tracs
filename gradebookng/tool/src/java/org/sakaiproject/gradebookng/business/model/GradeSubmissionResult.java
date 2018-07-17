package org.sakaiproject.gradebookng.business.model;

import java.io.Serializable;

import lombok.Getter;
import lombok.Setter;

public class GradeSubmissionResult implements Serializable{

		private static final long serialVersionUID = 1L;

		@Getter
		@Setter
		private Integer status;

		@Getter
		@Setter
		private String data;
}
