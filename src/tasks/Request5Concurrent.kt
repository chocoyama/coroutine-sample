package tasks

import contributors.*
import kotlinx.coroutines.*

// MEMO:
// KotlinのCoroutineは軽量であるため、非同期で新しい処理を行う必要がある都度、新しいCoroutineを生成しても問題ない
// その際、launch, async, runBlockingなどのCoroutineBuilderを利用して生成する
// - async:
//      - Deferredオブジェクトを返却する（Future, Promiseと同様の概念）
//      - Deferredは総称型で、Jobを継承している
//            - await()を呼び出すことで結果の返却を待機することが可能
//            - awaitAll()でListに対して全ての結果を待機することもできる
// - launch:
//      - asyncとは異なり、特定の返却値を期待しない場合に利用する
//      - launchはJobオブジェクトを返却し、join()を呼び出すことで完了まで待機することが可能
// - runBlocking:
//      - 通常の関数（ブロッキング）とsuspend関数（ノンブロッキング）の繋ぎ目で利用する
// CoroutineBuilderを利用することで、コールバックを必要とせずに非同期の複数リクエストができるようになる
//
// また、特定のスレッドで動作させたい場合はCoroutineDispatcher（Coroutineを実行するスレッドを指定するもの）を利用する
// - 未指定
//     - 呼び出し元のスレッドを利用する
// - Dispatchers.Default
//     - JVMの共有スレッドプール
//     - 使用可能なCPUのコアと同じ数のスレッドで構成される（ただし、コアが1つの場合は2つのスレッドが存在する）
// - Dispatchers.Main
//     - メインスレッド
//
// suspend関数内でCoroutineDispatcherを指定するよりも、呼び出し側で指定する方が柔軟に利用することができるので良い
//     launch(Dispatchers.Default) {
//         val users = loadContributorsConcurrent(service, req)
//         withContext(Dispatchers.Main) {
//             updateResults(users, startTime)
//         }
//     }
// ※ 確実に特定のスレッドで実行させたい場合はsuspend関数内で指定しるのもあり
//     suspend fun refreshTitle() {
//        withContext(Dispatchers.IO) {
//            try {
//                val result = network.fetchNewWelcome().await()
//                titleDao.insertTitle(Title(result))
//            } catch (error: FakeNetworkException) {
//                throw TitleRefreshError(error)
//            }
//        }
//    }
suspend fun loadContributorsConcurrent(service: GitHubService, req: RequestData): List<User> = coroutineScope {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()

    val deferreds: List<Deferred<List<User>>> = repos.map { repo ->
        async {
            service.getRepoContributors(req.org, repo.name)
                .also { logUsers(repo, it) }
                .bodyList()
        }
    }

    deferreds.awaitAll().flatten().aggregate()
}