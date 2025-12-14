from stable_baselines3 import PPO
from api_env import ApiEnv

EPISODE_LENGTH_MULTIPLIER = 20

env = ApiEnv()
env.reset()

# Steps for episode are the squared of the possible actions
steps = EPISODE_LENGTH_MULTIPLIER * env.actions_count

# Rounded to the closest multiple of 64 (the mini batch size of PPO)
if steps % 64 != 0:
    steps = steps - (steps % 64) + 64

model = PPO(
    policy='MlpPolicy',
    env=env,
    verbose=1,
    n_steps=steps
)

model.learn(total_timesteps=102400)