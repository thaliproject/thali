---
title: Thali Guide to Git
layout: default
---

# Read this first! 

Git is absurdly powerful. The unfortunate consequence of this power is that easy things are, in my opinion, hard. So, for example, it's pretty much impossible to write a short guide to how to handle basic Git commands for Thali because Git's power means that all sorts of things can go wrong. So please treat this guide as more along the lines of hints and pointers.

# Software (for Windows) 

For Windows I use the [Git Client](http://git-scm.com/download/win). Just download and run the installer. I use the default options when installing.

In most cases it's easiest (with a few exceptions I'll mention later) to use the Git Bash shell. You can run this shell from start. But this shell really wants to think it's in UNIX land, not Windows. So, for example, let's say you want to navigate to c:\temp. To do that you have to use 'cd /c/temp'. Also dir won't work, you need to use ls. 'del' won't work, you need to use 'rm', etc. Honestly I would just focus on 'cd' and 'ls'. If you need to mess around with files directly you can just use the explorer.

# Fork! 

For lots of reasons I'm happy to discuss we want our developers to fork their own version of the code and work there. So once you know which of our repositories you need to play with go to their links in codeplex and click on 'source code' and 'fork' and select 'create new fork'. Give it some pithy name and a description and hit save. You are now in your new home for doing your work. 

# Clone!

To do real work however you will need to create a local clone of your forked repository on your dev machine. In your forked project, click on the clone button and copy the https URL there. Then open up your favorite Git client (on Windows I do this from Git Bash shell) and type in:
 git clone [the URL]

This will automatically create a sub-directory whose name is equal to the last part of the path in the URL you copied. Now cd into that directory and you are in your own personal depot!

There are now three repositories you care about.

<dl>
<dt> Master Repository</dt>
<dd> This is the repository that contains the actual Thali code.</dd>
<dt> Forked Repository</dt>
<dd> This is the repository you created by forking the master repository, it starts off as a clone of the master repository</dd>
<dt> Cloned (or local) Repository</dt>
<dd> This is the repository you created on your dev machine, it starts off as a clone of the forked repository</dd>
</dl>

When you cloned your forked repository on your dev box Git understood it was a clone and automatically set up the identity of the forked repository. It calls your forked repository 'origin'.

But what about the master repository? As we'll discuss below there are times you will need to pull down changes from it. To enable that we need to tell your local repository about the master repository. Open up the Git Bash shell, navigate to the root of your local repository and type in: 

<pre>
 git remote add upstream [url of master repository]
</pre>

What the previous command did was tell your local repository that there is another repository it will know as 'upstream' (this is the traditional name, just like 'origin' is the traditional name for your forked repository).

# Branching! 

Go back to the [Git book](http://git-scm.com/book) and re-read the section on branching. If you don't understand how Git handles branching then, to be honest, you're doomed. It's foundational knowledge. Having read that section you understanding that 'master' is the default main branch in all Git repositories. Most folks (including Thali) keep Master as the main branch. But if you want to play around with something potentially messy it is often much easier to just create a branch and play there. (You can also check out staging as another play mechanism)

To create a new branch issue: 

<pre>
 git checkout -b [name]
</pre>

To switch to an existing branch issue: 

<pre>
 git checkout [name]
</pre>

BUT BE CAREFUL! I can't tell you how many times I screwed things up because I didn't realize I was on some branch other than my main one. Again, PLEASE read the section in the book on branching. Understanding how branching works, how it deals with changes, etc. is table stakes for using Git.

# Updating! 

When you have made changes you are happy with and want to persist them there are several steps you have to go through.

First, you want to commit those changes on your local repository. To do that I find it easiest to open the Git GUI application, hit 'open existing repository' and point it at the root directory of local repository. It will then show me all the unstaged and staged changes.

<dl>
<dt> Staged Change</dt>
<dd>
 A change that someone has told Git to pay attention to when it commits. IDEs will often detect when they are inside a Git repository (there are standard files to look for) and will automatically stage changed files. But not in all cases. So you might see unstaged changes.</dd>
<dt> Unstaged Change</dt>
<dd> A change that Git hasn't been told to pay attention to. If you did a commit when there are outstanding unstaged changed then they will be ignored by the commit. Generally having unstaged changes isn't a good idea. Yes, there are scenarios where it's fine. But personally I don't run into them a whole lot. So if you see unstaged changes in the UI look at them and either roll them back or stage them. 
</dd>
</dl>

To rollback a file: 

<pre>
 git checkout -- [name of file]
</pre>

To stage an unstaged file either select it in the Git Gui 'unstaged changes' section and hit commit->staged to commit or go to the command line and issue: 

<pre>
 git add [file]
</pre>
Note that if you accidentally staged a file you hadn't intended to you can use the UX (commit->unstage from commit) to remove it from staging.

If you make changes to file state in Bash when the Gui is open, have no fear, just hit the 'rescan' button and the Gui will update itself.

Once I have everything staged that I want to update I then type in a Commit Message and hit 'Commit'. This will update the local Git data to let it know you have a new commit and it will record the file state. But that change only exists on your local machine as part of your local repository.

You will also want to update your forked repository. To do that just hit the 'Push' button on the Git Gui. It will ask your name and password and update your forked repository.

Now you have pushed your changes to your depot and they are backed up remotely!

Note, btw, that if you don't like using the GUI another way to make this work is from the Git Bash Shell.

<pre>
 git status
</pre>

This will show you what's going on in your repository, which files are staged, which aren't and how they have changed.

<pre>
 git commit -a
</pre>

This is a blind command to stage everything that is unstaged. Obviously be careful. This will also open up a VI editor to let you write in a commit message. Move your cursor to the top of the screen using arrow keys and press 'i' to insert and type text. When you are done hit 'esc' to exit insert mode and type ':wq' to save out your changes and exit.

<pre>
 git push origin master
</pre>

This will upload your changes to your forked repository. You will need your name and password.

# Installing Software to help you Merge 

Merging is one of the hazards of doing development and good tools are essential. Git uses a three way merge paradigm where it looks at the newest common ancestor of the local branch and the remote branch and then compares both files against that. Doing this without a GUI is possible but really no fun. The best GUI I've found so far to do this is free (as in beer) and available [here](http://www.perforce.com/product/components/perforce-visual-merge-and-diff-tools) is called P4Merge. 

You have to tell Git to use P4merge as your merge tool. To do that issue these commands taken from [here](http://stackoverflow.com/questions/426026/git-on-windows-how-do-you-set-up-a-mergetool):

<pre>
 git config --global merge.tool p4merge
 git config --global mergetool.p4merge.cmd 'p4merge.exe \"$BASE\" \"$LOCAL\" \"$REMOTE\" \"$MERGED\"'
</pre>

WARNING: I used the above without incident on Windows 7 but when I moved over to Windows 8.1 I ran into errors, apparently because of the quoting. So on Windows 8, per [http://stackoverflow.com/questions/866262/p4merge-error-git](http://stackoverflow.com/questions/866262/p4merge-error-git) I use:

<pre>
 git config --global merge.tool p4merge
 git config --global mergetool.p4merge.cmd 'p4merge $BASE $LOCAL $REMOTE $MERGED'
</pre>

Note however that the person who put up the stack overflow answer said they needed to remote the quotes on Windows 7. So your mileage may vary.

# Keeping in Synch with the Upstream Repository 

Hopefully there are multiple developers working on the upstream repository and changes they make are not automatically pushed to your forked repository much less your local cloned repository. So to keep in synch with the upstream repository you have to pull down any changes to your local cloned repository of your fork, merge them and then push the result up to your forked repository.

This process is intentionally manual so that your code doesn't change unexpectedly underneath you!

<pre>
 git fetch upstream
</pre>

This will cause all changes in the upstream repository to be copied down to the local cloned repository of your fork. But those changes are just sitting in a hidden subdirectory. Nothing has actually happened with them yet.

<pre>
 git merge upstream/master
</pre>

The above command tests your understanding of branches. When you issued 'git fetch upstream' you pulled down all changes on all branches from the upstream repository. The second command, git merge upstream/master, says to apply changes specifically from the master branch. It's possible in some cases you will want to apply changes from other branches, but not likely for now.

Git does its best to resolve any conflicts but if it can't figure a conflict out then it will throw a merge error.

<pre>
 git mergetool
</pre>

This will start to walk you through various kinds of conflicts and ask you to resolve them. In the case of file version conflicts it will start up your merge tool (which you configured above).

Note that mergetool generally can only handle text conflicts, not binary conflicts. To handle binary conflicts I use:

<pre>
 git checkout --[ours || theirs] [conflicted file]
 git add [conflicted file]
</pre>

This tells the system to either pick "--ours" or "--theirs" and to add that file.

Note that the merge process only ends when you commit the changes. I use the Git Gui to do that.

# Using P4Merge 

To be honest this isn't the easiest merge tool I've ever seen. The UX is slick and its algorithms are very reasonable. The thing that confuses me is how you select which change you want. It will show you the more recent common ancestor in the middle (usually in yellow) and your changes on the left (in purple) and the remote changes on the right (in green) with a combined version on the bottom. 

In cases highlighted in red P4merge couldn't figure out how to resolve a conflict. In that case you have many choices. But what they boil down to is that you can select the colored icons on the right to decide which text from which of the three possible sources you want to keep. When you click on one of the icons it selects only that icon's text and nothing else. If you want to include multiple sources then you have to shift click to pick multiple icons. You can also go into the merged version and manually type things in. I suggest looking at the help if you want to figure out what all the little icons mean (because there are a bunch of them).

When you are done make sure to hit the disk icon to save and then exit the P4Merge tool. Git will detect the save and continue with the merge process. The merge process leaves around a variety of files to track the original data. You will see them when you are getting ready to commit as unstaged changes. Just delete those files once the merge is done.

# Keeping in Synch with your Forked Repository 

If you have only one dev machine and are the only person working in your forked repository then you can't really get out of synch with the forked repository. But if you are using multiple machines or if more than one person is working in your forked repository then you will need to pull down changes from the forked repository just like the master repository. The commands and processes are the same as in the previous section except 'upstream' is replaced with 'origin'.

# The ultimate goal - a Pull request! 

So your code in your forked repository is done and you are ready to have it moved to the master repository! Congratulations! That's what this is all about! To get your code submitting you create a pull request.

Generally the goal when submitting a pull request is to make a 'clean' request. That is, nobody (generally) is interested in the entire history of everything you have done. What they want is a pull request that will add exactly one commit to their history, the one with your changes.

How I handle this depends on how big my pull request is and how far ahead of the upstream repository I am.
## My branch contains a superset of what I want to submit in the pull request 

An example of this situation is CouchBase Lite. I have a branch of their depots that contains a whole list of changes that CouchBase isn't ready to deal with. So instead they ask me to submit them one by one over a long period of time. Since I can't stop developing it means that my personal branch is way, way ahead of what is in the upstream branch. So when I do pull requests with them the way I handle it is by creating a new local branch, synching it to the current state of the upstream branch and then making the changes I want in the pull request.

This looks as follows:

<pre>
 git checkout -b issue36
</pre>

This creates and switches to a new branch, issue36, which is the issue that I'm resolving in this example.

<pre>
 git fetch upstream [Note: I need to experiment to see if this command is really necessary]
 git reset upstream/master
</pre>

These commands pull down the latest stream of the upstream depo and then resets my state for this branch to be identical. So I am now dealing with what should be an identical copy to the state of the upstream master branch.

<pre>
 Edit Stuff
</pre>

Now I edit whatever files I need for the pull request. In the case of issue 36 this was a minor change to the build.gradle file.

<pre>
 git add [new stuff]
 git commit
 git push origin issue36
</pre>

Now I issue commands like git add to add any files that aren't tracked and do a local commit. Usually I handle this part, where I've finished making my changes and want to commit in preparation for the pull request, using the Git GUI using the commit/push routine.

<pre>
 Go to github and issue the Pull Request!
</pre>

And we are done!

# My main branch contains what I want to submit in the pull request 

In most cases I need to make tiny changes to existing libraries to add new features or fix bugs. In this case I typically want to submit all of my changes in the pull request. This is different than above where I have a tidal wave of changes in my branch but only want to submit a tiny subset in the pull request.

<pre>
 git fetch upstream
 git merge upstream/master
 git mergetool
</pre>

Most of the time these commands do nothing as I'm usually already in synch with the upstream repo. But if not this is good hygiene to do before a pull request so you make sure you aren't about to submit code that is behind the upstream. But once I'm done my branch is now fully sync'd and up to date with the master as well as having any of my changes on top.

<pre>
 git checkout -b proposal
</pre>

I now create a new branch from which I will make the pull request.

<pre>
 git rebase upstream/master
</pre>

This command takes all the commits I've created and resets them to be ahead of the HEAD of the upstream's master branch. This will set up me up to make a clean pull request by making sure that my history comes after whatever has gone on in upstream. A command like this would be a screaming nightmare in the case of CouchBase because I have so many changes trying to dig through them all to find what I want to submit and getting rid of everything else would be error prone and take forever. So in CouchBase's case it's easier to just copy over what I need. But for lots of other forks the changes are small and I want to submit them all. So with this command all my changes are now teed up quite nicely.

<pre>
 git add [stuff]
 git commit
</pre>

These commands will add my changes from the current state of the master branch as a commit on the local branch. But in some cases I don't need these commands. The reason is that I often check in my changes to my origin for safe keeping. If the upstream hasn't changed since those commits then the rebase above doesn't do anything.

<pre>
 git rebase -i HEAD~N
</pre>

This command is needed in the case that I have committed changes to origin. In that case I now have a visible history to deal with. The folks getting the pull request aren't going to be particularly interested in my history making a mess of their history. So I want to crunch down my history to a single commit. That's what this command lets me do. I check (usually using the history function in the GIT GUI) how many commits I have and enter that number in N. I then leave the first entry in VI alone and mark all the other entries with either a f (if I want to lose the commit statements) or a s (if I want to keep those commit statements). 

<pre>
 git push origin proposal
</pre>

I use the GIT GUI here normally but this is the point where I check in my pull branch to my depot.

<pre>
 Go to github and issue the Pull Request!
</pre>

And we are done!

# Where to from here? 
Git is vastly more powerful than what is described here. And there are often reasons to do much more interesting things, especially with branches. This guide is just meant as a simple 'getting started' for folks working on Thali.
