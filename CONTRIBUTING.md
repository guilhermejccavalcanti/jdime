$ git remote add upstream https://github.com/xai/jdime.git
$ git fetch upstream

then: (like "git pull" which is fetch + merge in the name of the branch)
$ git merge upstream/master master

or, better, replay your local work on top of the fetched branch
like a "git pull --rebase"
$ git rebase upstream/master