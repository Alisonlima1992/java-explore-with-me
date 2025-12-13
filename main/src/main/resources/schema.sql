-- Индексы для таблицы users
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_name ON users(name);

-- Индексы для таблицы categories
CREATE INDEX IF NOT EXISTS idx_categories_name ON categories(name);

-- Индексы для таблицы events
CREATE INDEX IF NOT EXISTS idx_events_state ON events(state);
CREATE INDEX IF NOT EXISTS idx_events_category_id ON events(category_id);
CREATE INDEX IF NOT EXISTS idx_events_event_date ON events(event_date);
CREATE INDEX IF NOT EXISTS idx_events_initiator_id ON events(initiator_id);
CREATE INDEX IF NOT EXISTS idx_events_published_on ON events(published_on);
CREATE INDEX IF NOT EXISTS idx_events_location ON events(location_lat, location_lon);
CREATE INDEX IF NOT EXISTS idx_events_created_on ON events(created_on);
CREATE INDEX IF NOT EXISTS idx_events_paid ON events(paid);
CREATE INDEX IF NOT EXISTS idx_events_participant_limit ON events(participant_limit);

-- Составной индекс для поиска событий
CREATE INDEX IF NOT EXISTS idx_events_search ON events(state, event_date, category_id);

-- Индексы для таблицы participation_requests
CREATE INDEX IF NOT EXISTS idx_requests_event_id ON participation_requests(event_id);
CREATE INDEX IF NOT EXISTS idx_requests_requester_id ON participation_requests(requester_id);
CREATE INDEX IF NOT EXISTS idx_requests_status ON participation_requests(status);
CREATE INDEX IF NOT EXISTS idx_requests_event_requester ON participation_requests(event_id, requester_id);
CREATE INDEX IF NOT EXISTS idx_requests_created ON participation_requests(created);

-- Индексы для таблицы compilations
CREATE INDEX IF NOT EXISTS idx_compilations_pinned ON compilations(pinned);
CREATE INDEX IF NOT EXISTS idx_compilations_title ON compilations(title);

-- Индексы для таблицы comments
CREATE INDEX IF NOT EXISTS idx_comments_event_id ON comments(event_id);
CREATE INDEX IF NOT EXISTS idx_comments_author_id ON comments(author_id);
CREATE INDEX IF NOT EXISTS idx_comments_created ON comments(created);
CREATE INDEX IF NOT EXISTS idx_comments_updated ON comments(updated);
CREATE INDEX IF NOT EXISTS idx_comments_event_created ON comments(event_id, created);

-- Таблица для связи многие-ко-многим compilation_events
CREATE INDEX IF NOT EXISTS idx_compilation_events_compilation_id ON compilation_events(compilation_id);
CREATE INDEX IF NOT EXISTS idx_compilation_events_event_id ON compilation_events(event_id);