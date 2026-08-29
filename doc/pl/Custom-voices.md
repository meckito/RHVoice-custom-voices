# Głosy własne (funkcja forka)

Ta strona opisuje funkcję, **której nie ma w oryginalnym RHVoice**: instalowanie
głosu z pliku, który już masz na urządzeniu, bez czekania, aż głos pojawi się
w oficjalnym katalogu.

Funkcja jest dostępna w forku
[meckito/RHVoice-custom-voices](https://github.com/meckito/RHVoice-custom-voices),
wyłącznie w aplikacji na Androida.

## Czego potrzebujesz, zanim zaczniesz

**Język głosu musi być już zainstalowany.** Głos zawiera wyłącznie modele
statystyczne mówiącego; reguły zamiany tekstu na dźwięki siedzą w danych
językowych. Jeśli spróbujesz zaimportować głos polski, nie mając zainstalowanego
języka polskiego, import zostanie odrzucony komunikatem, który wprost nazwie
brakujący język — zainstaluj wtedy ten język na ekranie głównym i powtórz import.

## Dwa sposoby importu

### Z wnętrza aplikacji

1. Otwórz aplikację.
2. Rozwiń menu w górnym pasku i wybierz **Głosy własne**.
3. Naciśnij **Zainstaluj głos z pliku**.
4. Wskaż plik w systemowym oknie wyboru.

### Z menedżera plików („Otwórz za pomocą")

Dotknij paczki głosu w dowolnym menedżerze plików i wybierz z listy aplikacji
**RHVoice Custom**. Pozycja jest podpisana *Zainstaluj głos z pliku*. Tak samo
działa udostępnienie pliku do aplikacji („Udostępnij" / „Wyślij do").

Aplikacja zgłasza się tylko do archiwów ZIP oraz do plików o nazwie kończącej się
na `.nvda-addon` — nie podpina się pod wszystkie pliki na urządzeniu.

## Obsługiwane układy paczek

Oba układy są przyjmowane i oba trafiają na dysk w identycznej postaci:

| Układ | Gdzie leży `voice.info` | Typowe źródło |
|---|---|---|
| paczka androidowa | w korzeniu archiwum | paczki głosów budowane dla aplikacji na Androida |
| dodatek NVDA | w podkatalogu, najczęściej `data/` | pliki `.nvda-addon` publikowane dla czytnika NVDA |

Z dodatku NVDA rozpakowywane jest **tylko poddrzewo głosu**. Wszystko, co należy do
opakowania dodatku — `manifest.ini`, `doc/` oraz dane językowe w `langdata/` — jest
pomijane. **Dane językowe nigdy nie są importowane**: ta funkcja instaluje wyłącznie
głosy.

Paczka musi zawierać plik `voice.info`, a w nim przynajmniej `name`, `language`
i `format`. Brak `revision` jest traktowany jako `0`.

## Zarządzanie zaimportowanymi głosami

Na ekranie **Głosy własne** każdy głos ma:

* przełącznik — wyłączenie ukrywa głos przed systemem, ale go nie usuwa,
* przycisk usuwania — pyta o potwierdzenie, a potem usuwa i wpis, i dane głosu.

Zmiany działają od razu, bez potrzeby ponownego uruchamiania usługi mowy.

## Dostępność

Funkcja została napisana z myślą o użytkownikach czytników ekranu:

* wynik importu pojawia się jako **trwały tekst na ekranie** w obszarze aktywnym
  (live region), a nie jako znikający komunikat — czytnik ogłasza go natychmiast,
  a Ty możesz do niego wrócić;
* przełącznik i przycisk usuwania mają w etykiecie **nazwę głosu**, więc przy kilku
  głosach na liście wiadomo, którego dotyczą;
* każdy element ma etykietę tekstową.

## Gdzie trzymane są głosy

Zaimportowane głosy leżą w prywatnej pamięci aplikacji, w katalogu celowo oddzielonym
od tego, w którym siedzą paczki pobrane z sieci — dzięki temu procedura czyszcząca
z oryginalnego kodu nigdy ich nie usunie:

```
<katalog prywatny apki>/app_local-voices/
    local-voices.properties    rejestr: id = nazwa|język|włączony
    <id głosu>/                dane głosu, z voice.info w korzeniu
```

Identyfikator głosu wyliczany jest z jego nazwy dokładnie tak, jak robi to oryginalny
kod (małe litery, `-` zamienione na `_`), więc głos zaimportowany i głos pobrany o tej
samej nazwie zajmują tę samą tożsamość.

## Bezpieczeństwo i zachowanie przy błędach

* **Wrogie archiwa są odrzucane.** Wpis, który próbuje zapisać plik poza katalogiem
  docelowym (`../`, ścieżki absolutne), dyskwalifikuje całą paczkę. Nic nie zostaje
  rozpakowane.
* **Nieudany import nigdy nie niszczy działającego głosu.** Rozpakowanie odbywa się
  w katalogu tymczasowym i wchodzi na miejsce docelowe dopiero na samym końcu, więc
  po awarii poprzednia wersja nadal działa.
* **Ponowny import zastępuje głos**, a nie dokłada się do starych plików, i zachowuje
  Twoją decyzję, jeśli wcześniej wyłączyłeś ten głos.
* **Pliki tymczasowe są zawsze sprzątane**, również po błędzie.
* Uszkodzony plik rejestru nie ukrywa głosów pobranych normalnie — silnik nadal
  dostaje wszystkie zainstalowane.

## Znane ograniczenia

* Jeden głos na plik; import wielu głosów naraz nie jest zrobiony.
* Zaimportowany głos nigdy nie staje się automatycznie głosem domyślnym.
* Głos musi odpowiadać językowi, który aplikacja zna z nazwy; głos dla języka
  nieobecnego w katalogu jest pomijany z ostrzeżeniem w logu.
