package org.example.wxbot.manager;

import lombok.extern.slf4j.Slf4j;
import org.example.wxbot.entity.UserLibrary;
import org.example.wxbot.exception.BusinessException;
import org.example.wxbot.mapper.UserLibraryMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@Slf4j
@Component
public class LibraryManager implements ApplicationRunner {

    private final Map<Integer, List<UserLibrary>> cache = new ConcurrentHashMap<>();
    private final UserLibraryMapper userLibraryMapper;
    private final ExecutorService botExecutor;

    public LibraryManager(
            UserLibraryMapper userLibraryMapper,
            @Qualifier("botExecutor") ExecutorService botExecutor
    ) {
        this.userLibraryMapper = userLibraryMapper;
        this.botExecutor = botExecutor;
    }

    @Override
    public void run(ApplicationArguments args) {
        botExecutor.submit(this::preload);
    }

    public List<UserLibrary> get(Integer userId) {
        if (userId == null) {
            throw new BusinessException("用户 ID 不能为空");
        }
        return cache.computeIfAbsent(userId, this::load);
    }

    public void put(UserLibrary library) {
        if (library == null || library.getUserId() == null) {
            throw new BusinessException("知识库数据不能为空");
        }
        cache.compute(library.getUserId(), (userId, current) -> {
            List<UserLibrary> next = current == null
                    ? new ArrayList<>()
                    : new ArrayList<>(current);
            next.removeIf(item -> item.getId() != null
                    && item.getId().equals(library.getId()));
            next.add(library);
            return List.copyOf(next);
        });
    }

    public void remove(Integer userId, Integer libraryId) {
        if (userId == null || libraryId == null) {
            return;
        }
        cache.computeIfPresent(userId, (id, current) -> current.stream()
                .filter(item -> !libraryId.equals(item.getId()))
                .toList());
    }

    public void evict(Integer userId) {
        if (userId != null) {
            cache.remove(userId);
        }
    }

    private void preload() {
        try {
            Map<Integer, List<UserLibrary>> grouped = userLibraryMapper.selectList(null)
                    .stream()
                    .collect(Collectors.groupingBy(
                            UserLibrary::getUserId,
                            Collectors.collectingAndThen(Collectors.toList(), List::copyOf)
                    ));
            grouped.forEach(cache::putIfAbsent);
            log.info("用户知识库缓存加载完成，共 {} 条", grouped.values().stream()
                    .mapToInt(List::size)
                    .sum());
        } catch (Exception exception) {
            log.error("用户知识库缓存加载失败，将在首次访问时回源", exception);
        }
    }

    private List<UserLibrary> load(Integer userId) {
        List<UserLibrary> libraries = userLibraryMapper.selectByUserId(userId);
        return libraries == null ? List.of() : List.copyOf(libraries);
    }
}
