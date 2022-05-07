# Steve

This is the main repository for Steve, a fake Docker clone that I'm making as part of an ongoing series on [my YouTube channel](https://www.youtube.com/channel/UCBSRCuGz9laxVv0rAnn2O9Q).

All development and design of this project is going to be recorded and published on YT,
with a schedule planned to be every week or two.

## Community implementations

These are the projects of my viewers who are following along (the ones I've found) - if you want yours added, send a PR! :)

- https://github.com/Bunyod/steve
- https://github.com/lgajowy/steve
- https://github.com/lenguyenthanh/steve
- https://github.com/miguel-vila/steve
- https://github.com/kmarreroe86/stevedore
- https://github.com/rmedjek/steve
- https://github.com/jhz7/steve

## [Episode 1](https://www.youtube.com/watch?v=EIE-6gx_qi0)

- Project setup
- Basic CI config
- Initial model design
- First piece of logic and test

## [Episode 2](https://www.youtube.com/watch?v=f4N8Xu2BVkA)

- Setup Scala Steward
- Initialize client
- Communicate with server

## [Episode 3](https://www.youtube.com/watch?v=e2Q3zU1lRkY)

- Build client with native-image
- Add logback to client
- Temporarily silence client errors

## [Episode 4](https://www.youtube.com/watch?v=mVU6rNmJNG0)

- Client/server compatibility testing
- Error handling

## [Episode 5](https://youtu.be/jBU7ZIrtPgU)

- Implement basic interpreter for build commands

## [Episode 6](https://www.youtube.com/watch?v=4AsH2k1MRjs)

- Initialize Registry, Resolver

## [Episode 7](https://www.youtube.com/watch?v=hyk245P6C3Q)

- Add tests for Registry/Resolver
- Stateless hash calculation
- Client method for listing images in the registry
- Tests for hashing
- New format of hashes

## [Episode 8](https://www.youtube.com/watch?v=4y-zvp8TKYU)

- Building client front-end

## [Episode 9](https://www.youtube.com/watch?v=GNleUFwgzWc)

- Initial support for streaming responses

## [Episode 10](...)

- Error handling in streaming
- Fixing stream safety (issue from episode 9)
- Fixing tests after streaming changes
- Minor refactoring in streaming

## Episode ???

- Clean up streaming
- (further) Extract logic from protocol
- Add messages to be streamed
- Pretty, streamed output?
- Custom build file format

- Tagging images
- Actual E2E tests
- More commands
- Migration to Weaver
- Logging unification
