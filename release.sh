#!/usr/bin/env bash
set -e

echo "Issuing 'git fetch origin' to get latest source from remote."
git fetch origin --quiet

HEAD_VERSION=$(git describe --tags origin/master)
LAST_VERSION=$(git describe --abbrev=0 --tags origin/master)
echo "HEAD version: $HEAD_VERSION"
echo "Last version: $LAST_VERSION"

if [ "$HEAD_VERSION" = "$LAST_VERSION" ]; then
    echo "No changes since last release. Bailing."
    exit 1
fi

regex='([[:digit:]]+)[\.-]([[:digit:]]+)'

if [[ $LAST_VERSION =~ $regex ]]; then
    LAST_VERSION_MAJOR="${BASH_REMATCH[1]}"
    LAST_VERSION_MINOR="${BASH_REMATCH[2]}"
else
    echo "Sorry. $LAST_VERSION does not look like a version number. bailing."
    exit 1
fi

TODAY=$(date +"%Y%m%d")
if [[ $LAST_VERSION == *"$TODAY"* ]]; then
    NEXT_VERSION="$TODAY.$((LAST_VERSION_MINOR + 1))"
else
    NEXT_VERSION="$TODAY.0"
fi

echo "Tagging with $NEXT_VERSION."
git tag $NEXT_VERSION -m $NEXT_VERSION origin/master
echo "Pushing master and $NEXT_VERSION."
git push origin $NEXT_VERSION master

DOCKER_TAG="ba-backoffice:$(git describe)"
IMAGE="gcr.io/hencjo/$DOCKER_TAG"
echo "Docker Tag: $DOCKER_TAG"
docker build -t $DOCKER_TAG .
docker tag $DOCKER_TAG $IMAGE
docker push $IMAGE
echo "$IMAGE pushed."
echo "Done."
