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
