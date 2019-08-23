package tasks

import contributors.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/*
MEMO:
- CoroutineはChannelを介して互いに通信をすることができる
    - Channelは、異なるCoroutine間でデータを受け渡すことができるもの
- あるCoroutine（Producer）がChannelに情報を送信すると、別のCoroutine（Consumer）がChannelからその情報を受信する
    - これは多対多のなりうる
        - ただし、複数のCoroutineがChannelから情報を受信する場合、それらの要素はConsumerのいずれかによって1度だけ処理される
            - その後、該当の要素は自動的にChannelから削除される
- Channelは3つの異なるインターフェースを介して表される
    - SendChannel
        - Producerが実装する
    - ReceiveChannel
        - Consumerが実装する
    - Channel
```
interface SendChannel<in E> {
    suspend fun send(element: E)
    fun close(): Boolean
}

interface ReceiveChannel<out E> {
    suspend fun receive(): E
}

interface Channel<E> : SendChannel<E>, ReceiveChannel<E>
```
- Channelにはいくつかの種類が存在する
    - 内部的に保存できる要素の数と、send呼び出しを中断できるかどうかが異なる
    - receiveについてはいずれも同様に動作し、Channelが空でない場合は要素を受け取り、それ以外の場合は中断する
- Channelの種類
    - Unlimited Channel
        - Queueに近い
            - 空になった時に、新しく要素が追加されるまで中断される点が異なる
        - メモリが枯渇するまで無限にsendすることができ、枯渇したタイミングでOutOfMemoryExceptionが投げられる
    - Buffered Channel
        - サイズを指定して利用する
        - サイズが一杯になると、次のsend呼び出しは空き容量ができるまで中断される
    - Rendezvous Channel
        - デフォルトで生成されるChannel
        - バッファのない（サイズ0の）Channel
        - 一方の関数はもう一方が呼び出されるまで常に中断される
            - 例えば
                - sendしてもreceiveされない場合、sendは中断される
                - 要素0の状態でreceiveを呼び出すと、sendが呼び出されるまで中断され続ける
    - Conflated Channel
        - バッファのない（サイズ0の）Channel
        - Channelに送信された要素は常に最新の値で上書きされる
            - そのため、sendが中断されることはない
```
val rendezvousChannel = Channel<String>()
val bufferedChannel = Channel<String>(10)
val conflatedChannel = Channel<String>(CONFLATED)
val unlimitedChannel = Channel<String>(UNLIMITED)
```
 */
suspend fun loadContributorsChannels(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    coroutineScope {
        val repos = service
            .getOrgRepos(req.org)
            .also { logRepos(req, it) }
            .bodyList()

        val channel = Channel<List<User>>()
        for (repo in repos) {
            launch {
                val users = service.getRepoContributors(req.org, repo.name)
                    .also { logUsers(repo, it) }
                    .bodyList()
                channel.send(users)
            }
        }
        var allUsers = emptyList<User>()
        repeat(repos.size) {
            val users = channel.receive()
            allUsers = (allUsers + users).aggregate()
            updateResults(allUsers, it == repos.lastIndex)
        }
    }
}
