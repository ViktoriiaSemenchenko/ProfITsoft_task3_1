# Завдання до лекцій 5-6
Модифікувати завдання з попереднього блоку (у папці є перелік текстових файлів, кожен із яких є "зліпок" БД порушень правил дорожнього руху протягом певного року...) таким чином, щоб різні файли з папки завантажувалися асинхронно за допомогою пулу потоків, але загальна статистика однаково формувалася.
Використовувати CompletableFuture і ExecutorService.
Порівняти швидкодію програми, коли не використовується параллелизація, коли використовується 2 потоки, 4 і 8.
Файлів в папці повинно бути 10+, їх розмір повинен бути достатнім, щоб заміри були цікавими.
Результати порівняння прикласти коментарем до викононаго завдання.

Результати порівняння: 8 потоків - 830 ms, 4 потоки - 923 ms, 2 потоки - 1188 ms. (результати можуть відрізнятися)
