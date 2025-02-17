#!/usr/bin/env bash

# Have script stop if there is an error
set -e

REPO=broadinstitute
PROJECT=gatk
REPO_PRJ=${REPO}/${PROJECT}
STAGING_CLONE_DIR=${PROJECT}_staging_temp

#################################################
# Parsing arguments
#################################################
while getopts "e:pslrud:" option; do
	case "$option" in
		e) GITHUB_TAG="$OPTARG" ;;
		p) IS_PUSH=true ;;
		s) IS_HASH=true ;;
		l) IS_NOT_LATEST=true ;;
		r) IS_NOT_REMOVE_UNIT_TEST_CONTAINER=true ;;
		u) IS_NOT_RUN_UNIT_TESTS=true ;;
		d) STAGING_DIR="$OPTARG" ;;
	esac
done

if [ -z "$GITHUB_TAG" ]; then
	printf "Option -e requires an argument.\n \
Usage: %s: -e <GITHUB_TAG> [-psl] \n \
where <GITHUB_TAG> is the github tag (or hash when -s is used) to use in building the docker image\n \
(e.g. bash build_docker.sh -e 1.0.0.0-alpha1.2.1)\n \
Optional arguments:  \n \
-s \t The GITHUB_TAG (-e parameter) is actually a github hash, not tag.  git hashes cannot be pushed as latest, so -l is implied.  \n \
-l \t Do not also push the image to the 'latest' tag. \n \
-u \t Do not run the unit tests. \n \
-d <STAGING_DIR> \t staging directory to grab code from repo and build the docker iamge.  If unspecified, then use whatever is in current dir (do not go to the repo).  NEVER SPECIFY YOUR WORKING DIR \n \
-p \t (GATK4 developers only) push image to docker hub once complete.  This will use the GITHUB_TAG in dockerhub as well. \n \
\t\t Unless -l is specified, this will also push this image to the 'latest' tag. \n \
-r \t (GATK4 developers only) Do not remove the unit test docker container.  This is useful for debugging failing unit tests. \n" $0
	exit 1
fi

# Make sure sudo or root was used.
if [ "$(whoami)" != "root" ]; then
	echo "You must have superuser privileges (through sudo or root user) to run this script"
	exit 1
fi

# -z is like "not -n"
if [ -z ${IS_NOT_LATEST} ] && [ -n "${IS_HASH}" ] && [ -n "${IS_PUSH}" ]; then
	echo -e "\n##################"
	echo " WARNING:  Refusing to push a hash as latest to dockerhub. "
	echo "##################"
	IS_NOT_LATEST=true
fi


# Output the parameters
echo -e "\n"
echo -e "github tag/hash: ${GITHUB_TAG}"
echo -e "docker hub repo, project, and tag: ${REPO_PRJ}:${GITHUB_TAG}\n\n"
echo "Other options (Blank is false)"
echo "---------------"
echo "This is a git hash: ${IS_HASH}"
echo "Push to dockerhub: ${IS_PUSH}"
echo "Do NOT remove the unit test container: ${IS_NOT_REMOVE_UNIT_TEST_CONTAINER}"
echo "Staging directory: ${STAGING_DIR}"
echo -e "Do NOT make this the default docker image: ${IS_NOT_LATEST}\n\n\n"

# Login to dockerhub
if [ -n "${IS_PUSH}" ]; then
	echo "Please login to dockerhub"
	docker login
fi

if [ -n "$STAGING_DIR" ]; then
    GITHUB_DIR="tags/"

    if [ -n "${IS_HASH}" ]; then
        GITHUB_DIR=" "
    fi

    mkdir -p ${STAGING_DIR}
    cd ${STAGING_DIR}
    set +e
    rm -Rf ${STAGING_DIR}/${STAGING_CLONE_DIR}
    set -e
    git clone https://github.com/${REPO}/${PROJECT}.git ${STAGING_DIR}/${STAGING_CLONE_DIR}
    cd ${STAGING_DIR}/${STAGING_CLONE_DIR}
    echo "Now in ${PWD}"
    GIT_CHECKOUT_COMMAND="git checkout ${GITHUB_DIR}${GITHUB_TAG}"
    echo "${GIT_CHECKOUT_COMMAND}"
    ${GIT_CHECKOUT_COMMAND}
fi


# Build
echo "Building image to tag ${REPO_PRJ}:${GITHUB_TAG}..."
docker build -t ${REPO_PRJ}:${GITHUB_TAG} .

if [ -z "${IS_NOT_RUN_UNIT_TESTS}" ] ; then

	# Run unit tests
	echo "Running unit tests..."
	REMOVE_CONTAINER_STRING=" --rm "
	if [ -n "${IS_NOT_REMOVE_UNIT_TEST_CONTAINER}" ] ; then
		REMOVE_CONTAINER_STRING=" "
	fi

	echo docker run ${REMOVE_CONTAINER_STRING} -t ${REPO_PRJ}:${GITHUB_TAG} bash /root/run_unit_tests.sh
	docker run ${REMOVE_CONTAINER_STRING} -t ${REPO_PRJ}:${GITHUB_TAG} bash /root/run_unit_tests.sh
	echo " Unit tests passed..."
fi

## Push
if [ -n "${IS_PUSH}" ]; then

	docker push ${REPO_PRJ}:${GITHUB_TAG}

	if [ -z "${IS_NOT_LATEST}" ] && [ -z "${IS_HASH}" ] ; then
		docker build -t ${REPO_PRJ}:latest .
		docker push ${REPO_PRJ}:latest
	fi

else
	echo "Not pushing to dockerhub"
fi

