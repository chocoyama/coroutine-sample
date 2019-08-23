package tasks

import contributors.GitHubService
import contributors.RequestData
import contributors.User
import kotlin.concurrent.thread

// MEMO:
// サブスレッドを生成して実行しているのでメインスレッドはブロックされないが、
// 生成したサブスレッドの中ではまだ直列処理が行われている。
// 複数のAPIリクエストがある時に、1つ目のリクエストが終わるまで2つ目のリクエストが行われない。
fun loadContributorsBackground(service: GitHubService, req: RequestData, updateResults: (List<User>) -> Unit) {
    thread {
        updateResults(loadContributorsBlocking(service, req))
    }
}