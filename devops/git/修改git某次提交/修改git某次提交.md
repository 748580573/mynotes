# 修改某次提交的Commit Message

### Git修改某次提交的[Commit](https://so.csdn.net/so/search?q=Commit&spm=1001.2101.3001.7020) Message

#### 背景

- CommitMessage指提交信息，如 git commit -m “Init code”，“Init Code”就是提交信息。
- 最后一条Commit，需要修改提交信息时，可以使用

````java
# 修改最后一条commit的提交信息
git commit --amend
````

- 如果想要修改的Commit为，提交记录中某次的提交，如：

````java
# 假设某个仓库有6个提交，想要修改 commit 3的提交信息
commit 5
commit 4
commit 3
commit 2
commit 1
commit 0
````

- **如上，想要修改 commit 3的提交信息，如何修改？**

#### Git 修改某一次提交的Commit Message

- 前提条件：某个仓库有6条提交记录。提交记录如下。出于某些原因，想要修改Commit3的提交信息。

````java
commit 5
commit 4
commit 3
commit 2
commit 1
commit 0
````

- 第一步：回退到 倒数第3次的状态(Commit 3，为倒数第3个提交)

````java
git rebase -i HEAD~3
````

- 第二步：执行完第一步后，在出现的编辑框中，将commit 3对应的那一条中的“pick”，修改为“edit”，并保存。

- 第三步：更新提交信息。

````java
# 使用该命令，更新提交信息
git commit --amend
````

- 第四步：恢复所有提交记录

````java
git rebase --continue
````

- 第五步：检查状态是否正确

````java
# 可通过git log查看提交记录、提交信息，是否正确。
git log
````

