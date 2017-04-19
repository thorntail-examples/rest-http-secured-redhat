# Checking out the Repository
To properly clone this repository, use the --recursive option to clone so that the sso submodule is checked out fully:

```
[sstark@sstark ObsidianToaster]$ git clone --recursive https://github.com/wildfly-swarm-openshiftio-boosters/rest-secured
Cloning into 'rest-secured'...
remote: Counting objects: 535, done.
remote: Compressing objects: 100% (3/3), done.
remote: Total 535 (delta 0), reused 0 (delta 0), pack-reused 532
Receiving objects: 100% (535/535), 92.67 KiB | 0 bytes/s, done.
Resolving deltas: 100% (193/193), done.
Submodule 'sso' (https://github.com/obsidian-toaster-quickstarts/redhat-sso) registered for path 'sso'
Cloning into 'sso'...
remote: Counting objects: 162, done.
remote: Total 162 (delta 0), reused 0 (delta 0), pack-reused 162
Receiving objects: 100% (162/162), 162.46 KiB | 0 bytes/s, done.
Resolving deltas: 100% (55/55), done.
Submodule path 'sso': checked out 'cd9fbf0980a3930ca7da1f1814a040bf5b032e96'
```

# Full Booster Experience Docs
See the full booster experience docs online at:

<http://openshiftio-appdev-docs-appdev-documentation.tsrv.devshift.net/mission-secured-wf-swarm.html>
