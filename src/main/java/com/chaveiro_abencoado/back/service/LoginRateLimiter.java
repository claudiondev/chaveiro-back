package com.chaveiro_abencoado.back.service;

import com.chaveiro_abencoado.back.exception.RateLimitException;

import java.util.LinkedHashMap;
import java.util.Map;

// Limitador de tentativas de login: uma instância por chave (e-mail ou IP de origem),
// nunca as duas coisas na mesma instância — AuthService mantém duas, independentes, para
// que um ataque com muitos e-mails distintos da mesma origem não esvazie o limite de
// contas legítimas, e vice-versa.
//
// Bounded por dois mecanismos, sem varredura completa do mapa a cada chamada:
//  - expiração por leitura: uma entrada expirada só é removida quando essa MESMA chave é
//    tocada de novo (O(1)); chaves nunca mais revisitadas não geram varredura em segundo
//    plano — ficam só até o mapa encher;
//  - tamanho máximo com descarte da entrada mais antiga (LRU): garante que o mapa nunca
//    cresce sem limite, mesmo com um atacante usando um e-mail (ou IP) diferente a cada
//    tentativa — é isso que fecha o buraco de memória do limitador antigo.
class LoginRateLimiter {

    private final int maxTentativas;
    private final long janelaMs;
    private final Map<String, Tentativas> porChave;

    LoginRateLimiter(int maxTentativas, long janelaMs, int capacidadeMaxima) {
        this.maxTentativas = maxTentativas;
        this.janelaMs = janelaMs;
        // LinkedHashMap com removeEldestEntry vira um LRU pronto; synchronized por fora
        // porque essa opção não é thread-safe sozinha (ao contrário de ConcurrentHashMap,
        // que não suporta o hook de descarte por tamanho).
        this.porChave = new LinkedHashMap<>(16, 0.75f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Tentativas> eldest) {
                return size() > capacidadeMaxima;
            }
        };
    }

    // Chama antes de tentar autenticar; lança se a chave estiver bloqueada
    synchronized void verificarBloqueio(String chave) {
        Tentativas atual = obterValida(chave);
        if (atual != null && atual.contagem >= maxTentativas) {
            long restanteMs = atual.expiraEm - System.currentTimeMillis();
            long minutosRestantes = Math.max(1, restanteMs / 60000 + (restanteMs % 60000 > 0 ? 1 : 0));
            throw new RateLimitException("Muitas tentativas. Tente novamente em " + minutosRestantes + " minuto(s)");
        }
    }

    synchronized void registrarFalha(String chave) {
        long agora = System.currentTimeMillis();
        Tentativas atual = obterValida(chave);
        if (atual == null) {
            porChave.put(chave, new Tentativas(1, agora + janelaMs));
        } else {
            porChave.put(chave, new Tentativas(atual.contagem + 1, atual.expiraEm));
        }
    }

    synchronized void limpar(String chave) {
        porChave.remove(chave);
    }

    // Devolve a entrada só se ainda não expirou; remove (O(1), só esta chave) se expirou
    private Tentativas obterValida(String chave) {
        Tentativas t = porChave.get(chave);
        if (t == null) return null;
        if (System.currentTimeMillis() >= t.expiraEm) {
            porChave.remove(chave);
            return null;
        }
        return t;
    }

    private static final class Tentativas {
        final int contagem;
        final long expiraEm;

        Tentativas(int contagem, long expiraEm) {
            this.contagem = contagem;
            this.expiraEm = expiraEm;
        }
    }
}
