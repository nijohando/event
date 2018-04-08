export WORK_DIR=target

# The following environment variables are required for some tasks.
# Please export them from a secure place other than Makefile
#export GPG_SIGN_CMD="gpg --batch --yes -b -a -u <user_name>"
#export DEPLOY_REPO_USER=<user_name>
#export DEPLOY_REPO_PASS=<password>

ifndef CLJCMD
	CLJCMD=clj
endif

TASK_CLJS=-C:dev -R:dev -m prj.task.cljs
TASK_TEST=-C:dev:test -R:dev:test -m prj.task.test
TASK_PACKAGE=-C:dev -R:dev:package -m prj.task.package
TASK_REPL=-C:dev:test -R:dev:repl:test:package -m prj.task.repl

GROUP_ID=jp.nijohando
ARTIFACT_ID=event
VERSION=0.1.1-SNAPSHOT
JAR_FILE=$(WORK_DIR)/$(ARTIFACT_ID)-$(VERSION).jar
DEPLOY_REPO_URL=https://clojars.org/repo

ifeq ($(findstring SNAPSHOT, $(VERSION)),)
	IS_RELEASE_VERSION=yes
endif

.PHONY: cljs/npm-install repl-clj repl-cljs test-clj test-cljs package deploy clean clean-all
.DEFAULT_GOAL := repl

npm-install:
	$(CLJCMD) $(TASK_CLJS) :npm-install

repl-clj:
	$(CLJCMD) $(TASK_REPL) :repl-clj

repl-cljs:
	$(CLJCMD) $(TASK_REPL) :repl-cljs

test-clj:
	$(CLJCMD) $(TASK_TEST) :test-clj

test-cljs:
	$(CLJCMD) $(TASK_TEST) :test-cljs

pom.xml:
	$(CLJCMD) -Spom
	$(CLJCMD) $(TASK_PACKAGE) :update-pom $(GROUP_ID) $(ARTIFACT_ID) $(VERSION)
ifdef IS_RELEASE_VERSION
	$(GPG_SIGN_CMD) pom.xml
endif

$(JAR_FILE):
	mkdir -p $(WORK_DIR)
	jar cf $(JAR_FILE) -C src jp
ifdef IS_RELEASE_VERSION
	$(GPG_SIGN_CMD) $(JAR_FILE)
endif

package: pom.xml $(JAR_FILE)

deploy: package
	$(CLJCMD) $(TASK_PACKAGE) :deploy pom.xml $(JAR_FILE) $(DEPLOY_REPO_URL)

clean:
	rm -rf ${WORK_DIR}
	rm -f pom.xml pom.xml.asc

clean-all: clean
	rm -rf ./node_modules

