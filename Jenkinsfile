node {
    stage('Check out') {
        checkout scm
    }  
    stage('Build') {
    	if (env.BRANCH_NAME == 'release') {
    		sh '''
	            cd source
	            $(which mvn) clean verify -Pprod
	        '''
    	} else {
    		sh '''
	            cd source
	            $(which mvn) clean verify
	        '''
    	}

        
    }
}
