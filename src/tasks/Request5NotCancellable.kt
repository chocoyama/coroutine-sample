package tasks

import contributors.*
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext

/*
MEMO:
- launch, async, runBlockingは自動的にCoroutineScopeを生成して、新しいCoroutineをスタートさせる
- 新しいCoroutineは、必ずCoroutineScope内でスタートされる必要がある
    - そのため、Coroutineを利用するにはメインスレッドをrunBlockingで開始させておく必要がある
        - ※ runBlockingは現在のスレッドをブロックする
    - CoroutineScope内で呼び出された、suspend関数はデフォルトでいわゆるawaitの挙動になる
- runBlocking内のスコープ（関数）内ではthisがCoroutineScopeを指しているので、launchは、実際にはthis.launchと同様の呼び出しとなっている
　そのため、ネストされたCoroutineには親子関係が存在することになる
  ```
  fun main() = runBlocking { scope ->
      scope.launch {
      }
  }
  ```
- suspend関数内で、coroutineScope関数を呼び出せば、該当の関数を呼び出したCoroutineScopeが利用されることになる
    - そのため、coroutineScope関数を利用していれば、新たなCoroutineを生成せずに処理を行うことができる
- 親子関係のある構造化された並行性（structured concurrency）には以下のような特徴がある
    - CoroutineScopeは一般的に子Coroutineを持ち、それらの生存期間はそのCoroutineScopeの生存期間と紐づく
    - 操作をキャンセルしたいときや、問題が発生した時など、CoroutineScopeは自動的に子Coroutineをキャンセルすることができる
    - CoroutineScopeは、全ての子Coroutineの完了を自動的に待機する
        - そのため、該当のスコープで起動された全てのCoroutineが完了するまでは、親Coroutineは完了しない
- また GlobalScope.async や GlobalScope.launch を使うことでトップレベルの独立したCoroutineScopeを生成することも可能
    - これは全て独立したスコープになるため、生存期間はアプリケーション全体の生存期間と紐づく
    - GlobalScopeから開始されたCoroutineの参照を保持し、その完了を待つか、明示的にキャンセルすることは可能
    - ただし、自動的にそれらが行われることはない
 */
suspend fun loadContributorsNotCancellable(service: GitHubService, req: RequestData): List<User> {
    val repos = service
        .getOrgRepos(req.org)
        .also { logRepos(req, it) }
        .bodyList()

    val deferreds: List<Deferred<List<User>>> = repos.map { repo ->
        GlobalScope.async {
            service.getRepoContributors(req.org, repo.name)
                .also { logUsers(repo, it) }
                .bodyList()
        }
    }

    return deferreds.awaitAll().flatten().aggregate()
}