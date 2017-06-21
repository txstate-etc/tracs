# TRACS BRANCHes

TRACS uses git branches for development and releases.

***master*** branch is the mirror of sakai's upstream master, ***should never*** work on it for local changes

***tracs*** branch is the most updated development branch for TRACS, this is the branch that have all the local changes for TRACS

***$feature-branch*** is a branch that branched off from ***tracs*** branch at certain time and focused on a particular feature.  Naming convention for feature branch is **$projectName-feature-name**  eg: forums-query-performance-fix

***$tracs-release-branch***  eg: 11.x will be tracs 11 release branch; will use tags for each release, eg: 11.4.1 

## Feature branch
We use feature branch to manage our local features development and also with consideration for potential contribution back to sakai community.

#### Feature branch workflow
1. Create New Feature branch

 *  Go to ***tracs*** branch

	 `git checkout tracs`

 * Create a new feature branch called ***kernel***

	  `git checkout -b kernel`

 * Do the work
 * Add changes

		`git add -u`

 * Make local commit

	  `git commit -m "Kernel local changes for tracs 11"`

 * Push feature branch ***kernel*** to origin

	  `git push origin kernel`

2. Merge feature branch with ***tracs***

 * Go to ***tracs*** branch

    `git checkout tracs`

 * Merge feature branch ***kernel*** to ***tracs*** branch

    `git merge kernel`

3. Tag and delete feature branch

   We need to clean up our branches list when when the feature merged back for ***tracs*** or abandon for some reason. Meanwhile, we also want to mark it somehow when time allows to work on contributing each local feature back to sakai by making pull requests.

   Right now, you are still at ***tracs*** branch after the merge is done in step 2.

 * Tag the feature branch ***kernel***
   We use ***archive/$feature-branch*** as naming convention for taging feature branch, mainly for contributing purpose.

    `git tag archive/kernel`

 * Close/Remove feature branch ***kernel***

    `git branch -d kernel`

 * Push the new archive tag to remote origin

    `git push origin archive/kernel`

 * Delete the feature branch ***kernel*** in remote origin

    `git push origin --delete kernel`

