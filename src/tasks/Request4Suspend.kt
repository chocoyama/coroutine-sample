package tasks

import contributors.*

// MEMO:
// メインスレッドで呼び出されているが、ブロックはしない
// 複数回連続で呼ばれた場合、メインスレッドはブロックされないが、前回のレスポンスを取得するまで後続のリクエストはブロックされる
suspend fun loadContributorsSuspend(service: GitHubService, req: RequestData): List<User> {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()

    return repos.flatMap { repo ->
        service.getRepoContributors(req.org, repo.name)
            .also { logUsers(repo, it) }
            .bodyList()
    }.aggregate()

//    val repos = service
//        .getOrgReposCall(req.org)
//        .execute() // Executes request and blocks the current thread
//        .also { logRepos(req, it) }
//        .bodyList()
//
//    return repos.flatMap { repo ->
//        service
//            .getRepoContributorsCall(req.org, repo.name)
//            .execute() // Executes request and blocks the current thread
//            .also { logUsers(repo, it) }
//            .bodyList()
//    }.aggregate()
}
