# MiniAnimator Studio 4.0

Revisione del ramo final/minianimator 3.1. La prima versione Studio si installa accanto all'app precedente con un identificativo distinto, senza modificarne i dati. I progetti JSON della versione 3.x possono essere importati da Apri; esportarli prima dalla vecchia app. I due archivi privati Android restano separati.

## Editor

Interfaccia scura adattabile a telefono e tablet, strumenti con icone e comandi per il tocco, foglio centrale, colori e livelli, timeline a miniature. Sui display stretti i pannelli laterali si aprono dal pulsante dedicato, lasciando spazio al foglio.

Pennello, matita, gomma trasparente, riempimento con tolleranza, contagocce, linea, rettangolo ed ellisse. Dimensione e opacità regolabili; pressione per le penne che la forniscono. Zoom, adattamento, spostamento e gesto a due dita. Sono inclusi testo rasterizzato, selezione rettangolare e spostamento dei pixel selezionati, riflessione dei livelli, ridimensionamento dell’intero documento ed effetti di base. Non sono implementati riconoscimento del palmo, lazo, trasformazione libera o un catalogo di pennelli professionali.

Livelli globali di animazione con visibilità, blocco, opacità, riordino, duplicazione e rinomina tramite doppio tocco sul nome. Aggiunta, duplicazione, eliminazione e riordino dei fotogrammi. Esposizione da 1 a 24 e riproduzione da 1 a 30 FPS. Onion skin precedente e successivo con colori distinti e opacità regolabile, escluso dalle esportazioni.

Annulla/rifai con storico limitato per contenere il consumo di memoria; ripristino automatico del progetto corrente tramite IndexedDB. Salvataggio manuale in un progetto JSON e apertura di progetti e immagini. Per conservare più progetti esportare i JSON: l'autosalvataggio mantiene soltanto il progetto corrente. Non è una sincronizzazione tra dispositivi.

## Esportazioni

PNG alla risoluzione del progetto, JPEG di alta qualità e BMP 24 bit su sfondo bianco. SVG con pennelli, forme e testo conservati come tracciati/elementi vettoriali; la gomma usa maschere SVG. Immagini importate, riempimenti, selezioni spostate, filtri e livelli ridimensionati/riflessi possono essere incorporati come immagini raster: non sono convertiti automaticamente in vettori. Il file JSON conserva anche i tracciati per la successiva esportazione SVG. GIF animata con timing ed esposizioni del progetto, fino a 640 px per lato e 40 milioni di pixel complessivi; GIF su sfondo bianco e codifica nativa fuori dal thread grafico. Il sistema Android chiede dove salvare. La conferma viene mostrata dopo la scrittura del file; l'annullamento non cancella il progetto.

Limiti della prima versione: 240 fotogrammi, 24 livelli e 2 megapixel per foglio. La memoria effettiva del dispositivo può richiedere progetti più piccoli. Non equivale alla dotazione completa di Clip Studio o FlipaClip. Audio, MP4, interpolazione, rigging e selezione/trasformazione avanzata non sono inclusi.

## Verifiche

Test del modello su celle, duplicazione e riordino, isolamento tra frame/livelli, durata, importazione precedente e annulla/rifai. Test del riempimento su confini, trasparenza e operazioni senza effetto. Test dei file BMP, dei tracciati e delle maschere SVG e della protezione XML. Test con il codice reale dell’editor e un motore Canvas su selezioni/spostamento, testo, trasformazioni, ridimensionamento, salvataggio, esportazioni e riapertura dei dati. Verifica della resa raster di un SVG esportato. Controllo della sintassi JavaScript e compilazione Android.

La prova reale su telefono/tablet e penna resta necessaria: tocco e pressione, zoom, ripristino, importazione, anteprima, esportazioni PNG/GIF e file salvati. Il codice dell'editor è suddiviso in model.js, pixels.js, studio.js e studio.css per consentire ulteriori revisioni senza duplicare un unico grande file.

## 4.0.1 — tema blu

Sfondo dell’editor, pannelli, barra superiore, finestre e barre Android convertiti a una palette blu. Il foglio e i colori dei disegni mantengono le impostazioni del progetto. Stesso identificativo e firma della Studio 4.0, versione Android superiore per aggiornare senza disinstallare.

## 5.0

Numerazione aggiornata su richiesta dell’utente: stessa revisione con tema blu, versioneName 5.0 e codice Android 50 per distinguere l’APK e consentire l’aggiornamento. Firma e identificativo della versione Studio invariati.

La firma visibile nell’interfaccia è “Creato da Dimitri Azzarone”, identica a quella verificata nel footer di LUME, inclusa anche nei formati di schermo più piccoli.

## 5.1 — Apri progetto e Importa immagine

Menu File con Nuovo/Apri/Salva progetto e pulsante Importa dedicato alle immagini. I progetti riaprono livelli e fotogrammi; le immagini si aggiungono al progetto attuale su un livello nuovo. Selettore Android con tipo di file specifico e lettura delle immagini tramite FileReader, anche se il fornitore non restituisce un MIME preciso. Nessuna sostituzione dei livelli esistenti durante l’importazione.

La versione 5.1 include inoltre apertura di GIF animate in un progetto a fotogrammi, con composizione dei frame parziali e gestione delle disposizioni GIF 2 e 3. I ritardi originali dei fotogrammi sono conservati in millisecondi, anche nella ri-esportazione; modificarne manualmente l’esposizione sostituisce il ritardo originale. Limiti di importazione: 30 MB, 240 fotogrammi, 2 megapixel per fotogramma, lato massimo 1920 e 24 milioni di pixel complessivi.

Copia, taglia e incolla usano appunti interni di MiniAnimator sul livello attivo o sulla selezione. Incolla crea un livello nuovo nel fotogramma scelto; annulla/rifai supporta queste operazioni. Non è prevista in questa versione la condivisione di immagini con gli appunti di altre app. La prospettiva di animazione 3D resta uno sviluppo futuro distinto dall’editor 2D.


## Studio 6.0 — spazio esteso e inquadratura

Il progetto conserva una scena di tratti vettoriali e immagini posizionati nello spazio; non viene creato un bitmap grande quanto l’intera area. La vista rasterizza solo la porzione visibile, entro circa 2 megapixel. Coordinate pratiche tra -1.000.000 e +1.000.000, zoom 1–3.200%, fino a 10.000 operazioni per livello/fotogramma. I limiti precedenti di fotogrammi/livelli e cronologia restano. Non chiamarla matematicamente infinita.

Il rettangolo azzurro è l’inquadratura esportata (fino a 1.920 px per lato e 2 megapixel). Pan e zoom non la spostano: Inquadra qui centra il rettangolo nella vista; Adatta torna al rettangolo e Mostra tutto comprende anche i disegni fuori dal rettangolo. Esportare il progetto JSON conserva tutta la scena, mentre PNG/JPEG/BMP/SVG/GIF esportano solo l’inquadratura. Importa vecchi progetti; i nuovi con scena estesa sono formato documento 5 e richiedono Studio 6.0 o successivo. Il salvataggio Android accetta formati 4 e 5.

Penna e dito: un dito disegna/cancella con lo strumento scelto; Solo penna ignora i tratti col dito; in entrambi i modi due dita spostano e ingrandiscono la vista. La penna ha priorità sui contatti tattili mentre disegna. Nessuna gomma automatica al contatto della mano. Mouse: rotella per zoom, mano o Spazio+trascinamento per spostare la vista. La pressione penna conserva l’opzione esistente.

La selezione magica ✦ prende la componente di pixel non trasparenti collegati del livello attivo, con vicinanza anche diagonale. Non è segmentazione AI di fotografie. Passa allo strumento spostamento dopo la selezione. Usa una maschera per lasciare ferme le parti non selezionate e gli altri livelli; mantiene semitrasparenza e supporta annulla/rifai. Selezioni raster fino a 2 megapixel; selezionare un’area più piccola quando serve. Copia/taglia/incolla conservano la selezione anche oltre l’inquadratura. Riempimento ed effetti raster sono delimitati all’area visibile/inquadratura e lo spazio esterno viene conservato; Svuota livello elimina tutto il contenuto di quel livello nel fotogramma.

Verifiche: test precedenti, test modello esteso e maschere, Chromium reale su desktop/telefono con disegno fuori pagina, reinquadratura, selezione/spostamento isolato, cronologia, entrambi gli input e gomma, gesti a due dita, JSON e IndexedDB, PNG/SVG con verifica pixel, JPEG/BMP e preparazione frame GIF, import immagini e fotogrammi nuovi/duplicati. Test penna/dito mediante eventi browser sintetici: serve il collaudo della penna reale sul tablet.
